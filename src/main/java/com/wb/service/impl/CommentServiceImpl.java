package com.wb.service.impl;

import com.wb.common.DateUtils;
import com.wb.entity.*;
import com.wb.mapper.CommentMapper;
import com.wb.mapper.CounterMapper;
import com.wb.service.CommentService;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.RedisTemplate;

import java.text.ParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 评论系统
 */
public class CommentServiceImpl implements CommentService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private CounterMapper counterMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private RedissonClient redisson;

    /**
     * 查询所有评论
     * @param objId
     * @return
     */
    @Override
    public List<Comment> query(int objId) {
        //查询
        Query query = new Query(Criteria.where("obj_id").is(objId).and("root").is(0));
        //返回数据
        List<Comment> comments = mongoTemplate.find(query, Comment.class);
        return comments;
    }

    /**
     * 分页查询
     * @param objId
     * @param page
     * @return
     */
    @Override
    public List<Comment> queryPage(int objId, int page) {
        //这里我们可以直接使用mongoTemplate来进行count查询
        Query query = new Query(Criteria.where("name").where("obj_id").is(objId).and("root").is(0));
        //查询总数
        //不过这种分页查询有一个问题，就是查询效率太低，大家用过mongoDB都知道，
        // 进行count聚合查询，然后分页查询效率肯定是很低的，mysql也是一个道理。
        //所以我们这里直接调用计数服务查询是ok的

        long count = mongoTemplate.count(query , Comment.class);
        //使用Redis进行查询count
        count = (long)redisTemplate.opsForValue().get("comment:" + objId);
        //分页查询
        List<Comment> comments = mongoTemplate.find(query.skip((page - 1) * 10).limit(10), Comment.class);

        return comments;
    }

    /**
     * 添加数据
     * @param comment
     * @return
     */
    @Override
    public int insert(Comment comment) {
        comment.setCreateTime(DateUtils.getDate(new Date()));
        comment.setLike(0);
        comment.setState(0);
        comment.setAttrs(0);
        //...
        //set一些属性值，然后直接保存在数据库当中就ok。当然这一步肯定也是异步来做，使用mq
        //但是在保存之前，我们需要进行几步操作，和发表文章一样，需要进行
        //黑白名单校验
        //文本审核
        //ai审核
        //人工审核
        //这几步审核完毕之后，才能算发表成功
        mongoTemplate.insert(comment);
        //如果是二级评论，则还需要修改父评论的总数
        if (comment.getFloor()==2){
            Comment root = new Comment();
            root.setId(comment.getParent());
            root.setCount(1);
            update(root);
        }
        return 0;
    }

    /**
     * 删除评论（发表者和该评论作者都可以删除）
     * @param comment
     * @return
     */
    @Override
    public int delete(Comment comment) {
        Query query = new Query(Criteria.where("_id").is(comment.getId()));
        Update update= new Update();
        update.set("attrs" , 1);
        mongoTemplate.upsert(query , update , Comment.class);
        //如果是二级评论删除评论不仅要删除这条评论，同时需要修改父级评论的子评论总数
        if (comment.getFloor()==2){
            Comment root = new Comment();
            root.setId(comment.getParent());
            root.setCount(-1);
            update(root);
        }
        //如果是一级评论被删除了，其实是不需要去管二级评论的，虽然数据还存在，但是查询的时候是查询不到的
        //当然调用都是异步来进行完成的
        return 0;
    }

    /**
     * 修改评论信息
     * @param comment
     * @return
     */
    @Override
    public int update(Comment comment) {
        Query query = new Query(Criteria.where("name").where("_id").is(comment.getId()));

        Update update = new Update();
        if (comment.getLike()!=null){
            update.inc("like" , comment.getLike());
        }
        if (comment.getCount()!=null){
            update.inc("count" , comment.getCount());
        }
        if (comment.getState()!=null){
            update.set("state" , comment.getState());
        }
        if (comment.getAttrs()!=null){
            update.set("attrs" , comment.getAttrs());
        }

        //判断完事之后，直接修改
        mongoTemplate.upsert(query , update , Comment.class);

        return 0;
    }

    /**
     * 分页查询根评论
     * @param objId
     * @param start
     * @param end
     * @return
     */
    @Override
    public List<CommentIndex> rootQueryPage(int objId, int start, int end) {
        //count
        //第一次查询的时候需要查询count，如何判断第一次呢，就从start开始，如果start是0就代表是第一次查询
        int rootCount = 0;
        if (start==0){
            rootCount = rootCount(objId);
            //如果根为空说明没有评论
            if (rootCount==0){
                List<CommentIndex> commentIndices = new ArrayList<>();
                return commentIndices;
            }
        }

        //根据start和end查询根列表
        List<CommentIndex> commentIndexList = commentIndices(objId, start, end);

        //调用判断方法
        is_like(commentIndexList);

        return commentIndexList;
    }

    /**
     * 分页查询二级
     * @param root
     * @param start
     * @param end
     * @return
     */
    @Override
    public List<CommentIndex> queryPage(int root, int start, int end) {

        //查询二级评论直接进行分页查询就ok了，因为count在父级评论上已经展示出来了
        Set set = redisTemplate.opsForZSet().range("comment:" + root, start, end);
        List<String> keys = new ArrayList<>();
        List<Integer> ids = new ArrayList<>();
        List<CommentIndex> commentIndices;
        if (set!=null){
            //列表查询出来之后进行数据拼装
            commentIndices = new ArrayList<>();
            commentIndices.addAll(set);

            for (CommentIndex commentIndex : commentIndices){
                keys.add("comment:"+commentIndex.getId());
                ids.add(commentIndex.getId());
            }

        }else{
            //说明列表不存在，需要查询数据库
            commentIndices = commentMapper.pageSecondComment(root, 0, 100000);
            if (commentIndices==null){
                //说明不存在，直接返回
                return  new ArrayList<>();
            }
            //循环10次，添加keys
            int i=0;
            for (CommentIndex commentIndex : commentIndices){
                if (i<10){
                    keys.add("comment:"+commentIndex.getId());
                    ids.add(commentIndex.getId());
                }
                //添加到redis当中
                //异步来做，因为zset是自动排序的，通过count或者时间
                try {
                    redisTemplate.opsForZSet().add("comment:" + root , commentIndex.getId() , DateUtils.getDate(commentIndex.getCreateTime()));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }

        //查询具体内容
        List<CommentIndex> list = redisTemplate.opsForValue().multiGet(keys);
        if (list!=null){
            //说明list数据是存在的
            ids = new ArrayList<>();
            List<Integer> nullIndexs = new ArrayList<>();
            int n=0;
            for (CommentIndex commentIndex : list){
                //循环判断某个元素是否为空
                if (commentIndex==null){
                    ids.add(commentIndices.get(n).getId());
                    nullIndexs.add(n);
                }
                n++;
            }

            if (ids.size()!=0){
                //说明有数据，直接去查询数据库
                List<CommentIndex> contents = commentMapper.contents(ids);
                for (int i=0;i<contents.size();i++){
                    list.add(nullIndexs.get(i) , contents.get(i));
                    //异步保存数据
                    redisTemplate.opsForValue().set("comment:"+contents.get(i).getId() , contents.get(i));
                }
                return list;
            }else{
                List<CommentIndex> contents = commentMapper.contents(ids);
                for (int i=0;i<contents.size();i++){
                    //异步保存数据
                    //对于比较老的数据，我们这里可以判断一下是否是非置顶的老数据
                    //如果是非置顶的老数据，比如一个月之前的数据，我们可以使用滑动窗口来统计是否需要缓存
                    redisTemplate.opsForValue().set("comment:"+contents.get(i).getId() , contents.get(i));
                }
                return contents;

            }

        }

        return null;
    }

    /**
     * 判断是否点赞过
     * @param commentIndices
     */
    @Override
    public void is_like(List<CommentIndex> commentIndices) {
        //为了方便演示，弄一个假的userId，正常直接获取就行了
        int userId = 1;

        //判断点赞总次数，如果点赞的次数不多的话，其实我们是可以直接使用列表来存储的
        for (CommentIndex commentIndex : commentIndices){
            if (commentIndex.getLike()>=1000){
                //这里假设点赞数量大于1000，去判断布隆过滤器
                RBloomFilter<Object> bloomFilter = redisson.getBloomFilter("bloom_like:"+commentIndex.getId());
                //布隆过滤器中存储的是用户id
                //判断数据是否在过滤器中，获取当前用户的userId
                boolean flag = bloomFilter.contains(userId);
                //判断
                if (!flag){
                    //说明没点赞过
                    commentIndex.setIs_like(0);
                }else{
                    //说明可能点赞过
                    //查询取消点赞列表
                    Boolean member = redisTemplate.opsForSet().isMember("no_like:" + commentIndex.getId(), userId);
                    if (member){
                        //说明取消点赞，那就说明没点赞
                        commentIndex.setIs_like(0);
                    }else{
                        //反之说明点赞过，如果不希望存在误判，可以去查询数据库
                        commentIndex.setIs_like(1);
                    }
                }

            }else{
                //说明点赞量比较少，直接存储在redis当中 set形式就行了
                Boolean member = redisTemplate.opsForSet().isMember("like:" + commentIndex.getId(), userId);
                if (!member){
                    //说明没点赞
                    commentIndex.setIs_like(0);
                }else{
                    //反之说明点赞
                    commentIndex.setIs_like(1);
                }
            }
        }
        //在布隆过滤器中进行运算，然后判断是否存在，如果不存在，说明一定没点赞过

    }

    /**
     * 查询根评论count
     * @param objId
     * @return
     */
    private int rootCount(int objId){
        //从redis中查询count
        int count = (int)redisTemplate.opsForValue().get("rootCommentCount:" + objId);
        //如果redis不为空，直接返回
        if (count!=0){
            return count;
        }
        //如果为空查询数据库
        Counter counter = counterMapper.getById(objId);
        if (counter!=null && counter.getRootCommentCount()!=0){
            //这里需要判断一下是否是非置顶的老数据
            redisTemplate.opsForValue().set("rootCommentCount:" + objId , counter.getRootCommentCount() , 24 , TimeUnit.HOURS);
            return counter.getRootCommentCount();
        }
        return 0;
    }

    /**
     * 分页查询跟评论信息
     * @param objId
     * @param start
     * @param end
     * @return
     */
    private List<CommentIndex> commentIndices(int objId,int start,int end){

        List<String> keys = new ArrayList<>();
        //分页查询根评论id列表
        Set<Integer> set = redisTemplate.opsForZSet().reverseRange("rootComment:" + objId, start, end);
        List<CommentIndex> commentIndexList = new ArrayList<>();
        //创建一个放id的集合
        List<Integer> ids = new ArrayList<>();
        //判断数据是否存在
        if (set!=null){
            for (Integer id:set){
                CommentIndex commentIndex = new CommentIndex();
                commentIndex.setId(id);
                commentIndexList.add(commentIndex);
                ids.add(id);
                keys.add("comment:"+commentIndex.getId());
            }
        }else{
            //如果set不存在，查询数据库,如果第一页没有数据的话，说明整个列表都没有数据，直接把objId下所有的索引都查询出来
            commentIndexList = commentMapper.rootComment(objId);
            //批量保存到redis当中，当然这里我们就可以异步的进行保存了，然后进行返回就ok了。一般这种跟评论数据也不会特别大
            for (CommentIndex commentIndex : commentIndexList){
                //这里可以异步来去做
                redisTemplate.opsForZSet().add("rootCommentCount:" + objId , commentIndex.getId() , commentIndex.getLike());
            }
            return commentIndexList;
        }
        List<CommentIndex> contents = redisTemplate.opsForValue().multiGet(keys);

        if (contents!=null){
            //判断具体对象是否为空
            int i=0;
            //记录一下位置数，然后批量查询
            List<Integer> nullIndexs = new ArrayList<>();
            ids = new ArrayList<>();
            //循环判断是否为空
            for (CommentIndex commentIndex : contents){
                if (commentIndex==null || commentIndex.getMessage().equals("")){
                    //查询数据库
                    //记录null的位置
                    nullIndexs.add(i);
                    //添加ids，方便后面查询数据
                    ids.add(commentIndex.getId());
                }
                i++;
            }
            //判断ids是否为空
            if (ids!=null){
                //说明缓存当中有过期的key，批量查询数据库
                List<CommentIndex> commentContents = commentMapper.contents(ids);
                for (int y=0;y<commentContents.size();y++){
                    //这里可以异步来去做
                    //当然，如果这个帖子是老帖子或者说是老视频这种，我们其实就可以不用放在缓存当中，直接去查询数据库就ok了
                    //所以这里我们可以判断一下时间+热度，如果是一个老帖子，那不需要放在缓存当中了
                    redisTemplate.opsForValue().set("comment:" + commentContents.get(y).getId() , commentContents.get(y) , 8 , TimeUnit.HOURS);
                    contents.add(nullIndexs.get(y) , commentContents.get(y));
                }
            }
            return contents;

        }else{
            //说明所有都为空
            ids = new ArrayList<>();
            int n=0;
            for (CommentIndex commentIndex : commentIndexList){
                if (n>=10){
                    break;
                }
                ids.add(commentIndex.getId());
                n++;
            }
            List<CommentIndex> commentContents = commentMapper.contents(ids);
            //因为批量保存kv需要map类型，创建hashmap，保存redis
            Map<String , CommentIndex> maps = new HashMap<>();
            for (CommentIndex commentIndex : commentContents){
                maps.put("comment:"+commentIndex.getId() , commentIndex);
            }
            redisTemplate.opsForValue().multiSet(maps);

            return contents;
        }

    }


    /**
     * 新增评论
     * @param commentIndex
     */
    @Override
    public void insert(CommentIndex commentIndex) {
        //调用数据库新增方法
        commentMapper.insertCommentIndex(commentIndex);
        if (commentIndex.getFloor()==1){
            //删除缓存列表
            redisTemplate.opsForZSet().remove("rootComment:"+commentIndex.getObjId());
        }else{
            redisTemplate.opsForZSet().remove("comment:"+commentIndex.getRoot());
        }
    }

    /**
     * 点赞评论
     * @param commentIndex
     */
    @Override
    public void like(CommentIndex commentIndex) {
        int userId = 1;
        //这里点赞评论的话，我们就更新列表就行了，因为不要求很强的一致性
        //新增数据库
        //判断帖子的点赞是否大于1000
        if (commentIndex.getLike()<999){
            redisTemplate.opsForSet().add("like:"+commentIndex.getId() , userId);
        }else if(commentIndex.getLike()==999){
            //这里就需要转换一下了，给set转换成布隆过滤器了
            //获取布隆过滤器
            RBloomFilter<Integer> bloomFilter = redisson.getBloomFilter("bloom_like:"+commentIndex.getId());
            //初始化布隆过滤器(数据量，误差率)
            bloomFilter.tryInit(1000000L,0.02);
            //往过滤器中加入数据
            bloomFilter.add(userId);
            Set<Integer> members = redisTemplate.opsForSet().members("like:" + commentIndex.getId());
            for (int uid : members){
                bloomFilter.add(uid);
            }
            redisTemplate.delete("like:"+commentIndex.getId());
        }else{
            RBloomFilter<Integer> bloomFilter = redisson.getBloomFilter("bloom_like:"+commentIndex.getId());
            bloomFilter.add(userId);
        }


    }

    /**
     * 修改或删除评论
     * @param commentIndex
     */
    @Override
    public void update(CommentIndex commentIndex) {
        //调用数据库修改方法
        commentMapper.updateCommentIndex(commentIndex);
        if (commentIndex.getState()==1){
            if (commentIndex.getFloor()==1){
                //删除缓存列表
                redisTemplate.delete("rootComment:"+commentIndex.getObjId());
            }else{
                redisTemplate.delete("secondComment:"+commentIndex.getRoot());
            }
            //删除content缓存
            redisTemplate.delete("comment:"+commentIndex.getId());
            //判断点赞数量
            if (commentIndex.getLike()>1000){
                //删除布隆过滤器
                redisTemplate.delete("bloom_like:"+commentIndex.getId());
                //删除取消点赞列表
                redisTemplate.delete("no_like:"+commentIndex.getId());
            }else{
                //删除点赞列表
                redisTemplate.delete("like:"+commentIndex.getId());
            }
        }

    }

    /**
     * 取消点赞
     * @param commentIndex
     */
    @Override
    public void noLike(CommentIndex commentIndex) {
        int userId = 0;
        //新增数据库
        //修改commentIndex like具体的值
        commentIndex.setLike(commentIndex.getLike()+1);
        update(commentIndex);
        if (commentIndex.getLike()>1000){
            redisTemplate.opsForSet().add("no_like:"+commentIndex.getId() , userId);
        }else{
            redisTemplate.opsForSet().remove("like:"+commentIndex.getId() , userId);
        }
    }
}
