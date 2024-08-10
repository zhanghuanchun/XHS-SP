package com.wb.service.impl;

import com.alibaba.fastjson.JSON;
import com.wb.common.Geocoder;
import com.wb.common.Geohash;
import com.wb.entity.Counter;
import com.wb.entity.PostIndex;
import com.wb.entity.Re;
import com.wb.mapper.CounterMapper;
import com.wb.mapper.PostMapper;
import com.wb.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PostServiceImpl implements PostService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private CounterMapper counterMapper;


    /**
     * 查询用户帖子列表
     * @param uid
     * @return
     */
    @Override
    public List<PostIndex> userPosts(int uid,int start,int stop) {
        //先分页查询redis，如果redis有直接返回
        List<String> posts = redisTemplate.opsForList().range("posts:" + uid, start, stop);
        List<String> ids = new ArrayList<>();
        List<PostIndex> postIndexList = new ArrayList<>();
        //然后循环查询拼装
        for (int i=0 ; i<posts.size(); i++){
            String postStr = posts.get(i);
            if (postStr!=null && !postStr.equals("")){
                //把string转换成对象
                PostIndex post = JSON.parseObject(postStr, PostIndex.class);
                ids.add("counters:" + post.getId());
                postIndexList.add(post);
            }
        }
        //mget进行查询点赞信息
        List<String> counterStr = redisTemplate.opsForValue().multiGet(ids);
        if (counterStr!=null){
            for (int i=0 ; i<counterStr.size() ; i++){
                String str = counterStr.get(i);
                if (str!=null && str.equals("")){
                    //阅读数：0:点赞数：0:转发数：0:0
                    String[] split = str.split(":");
                    //获取第二个计数数据也就是点赞计数
                    PostIndex postIndex = postIndexList.get(i);
                    postIndex.setPraiseCount(Integer.parseInt(split[1]));
                }else{
                    //如果点赞计数为空就需要查询mysql了呗
                    //这里和上面一样，查询出来然后进行拼装就ok了。
                }
            }

        }else {
            //所有的点赞都未空的话，说明这个用户他可能是一个比较冷的用户对吧
            //直接批量查询mysql把这些点赞进行查询出来进行拼装就ok了
            //点赞我们再存入到redis当中呢？
            //我们会有热点探测
        }
        return postIndexList;
    }

    /**
     * 添加附近的帖子
     * @param postId
     * @param latitude
     * @param longitude
     */
    @Override
    public void insNearbyPost(int postId, double latitude, double longitude) {
        //附近的帖子Redis没有过期这一说，所以我们这里设置一个Redis最大容量就行了，这个前面已经给大家去讲过了
        //这里通过地理位置我们可以判断出来
        try {
            String province = Geocoder.getProvince(latitude, longitude);
            //添加到redis当中
            redisTemplate.opsForList().leftPush("nearby:"+province , postId);
            //判断是否需要弹出，这里我们最高只让他保存1000个，如果帖子比较多，我们可以分成两个或者三个list，随机保存
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 查询附近的帖子
     * @param uid
     * @param latitude
     * @param longitude
     * @return
     */
    @Override
    public List<PostIndex> nearbys(int uid , double latitude, double longitude) {
        //这里需要获取两个参数，一个是用户的uid还有一个是用户目前所在的地理位置
        List<Integer> ids = new ArrayList<>();
        try {
            //我们使用redis进行存储的时候，我们实际上是不同的省份我们都各存储一个redis
            //通过用户的经纬度获取省份
            String province = Geocoder.getProvince(latitude, longitude);
            //通过省份去查询Redis
            ids = redisTemplate.opsForList().range("nearby:" + province, 0, 10);
            //用力批量查询redis
            List<String> strList = ids.stream()
                    .map(i -> "post:" + i.toString())
                    .collect(Collectors.toList());
            List<PostIndex> list = redisTemplate.opsForValue().multiGet(strList);

            List<Integer> nullIndexes = new ArrayList<>();
            //看内容是否为空，如果为空记录位置
            for (int i = 0; i < list.size(); i++) {
                PostIndex val = list.get(i);
                if (val == null) {
                    nullIndexes.add(i);
                }
            }
            //查询数据库赋值到空位置即可

            //循环来计算距离
            for (PostIndex value : list){
                double distance = Geohash.getDistance(latitude, longitude, value.getLatitude(), value.getLongitude());
                //四舍五入保留成整数
                value.setDistance(Math.round(distance));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
