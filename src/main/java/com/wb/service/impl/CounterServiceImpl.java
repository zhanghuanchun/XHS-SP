package com.wb.service.impl;

import com.github.phantomthief.collection.BufferTrigger;
import com.wb.entity.Counter;
import com.wb.entity.Praise;
import com.wb.entity.Re;
import com.wb.entity.User;
import com.wb.mapper.CounterMapper;
import com.wb.service.CounterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.cache.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.annotation.Async;

public class CounterServiceImpl implements CounterService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private CounterMapper counterMapper;

    //key：id value：bufferTrigger
    private Map<Integer , BufferTrigger<Long>> bufferTriggerMap = new HashMap<>();


    private void consume(List<Long> nums) {

    }

    private void setBuffer(int id){
        BufferTrigger<Long> orDefault = bufferTriggerMap.getOrDefault(id, BufferTrigger.<Long>batchBlocking()
                .bufferSize(50)
                .batchSize(10)
                .linger(Duration.ofSeconds(1))
                .setConsumerEx(this::consume)
                .build());
        orDefault.enqueue(1L);
    }


    /**
     * 更新计数在Redis当中进行更新
     * @param id
     * @return
     */
    @Override
    public int updateReadCount(int id) {
        //0:0:0:0
        //字符串形式来进行存储，你也可以使用hash来进行存储
        //使用k-v来进行存储和获取了
        Integer value = (Integer)redisTemplate.opsForValue().get("readCount:" + id);
        if (value != null) {
            // 如果key存在，则将其值加1，并保存回Redis
            redisTemplate.opsForValue().set("readCount", value + 1);
        } else {
            //这里有两种情况，第一种可能是Redis缓存过期了，还有就是没创建
            //但是在创建帖子的时候，会创建readCount为0，所以这里大概率是过期了。
            //滑动窗口，热点探测，jdhotkey
            //那这里就不需要去管了，让他去批量添加数据库就行了
            // 如果key不存在，则创建一个新的key，并将其值设置为1
            redisTemplate.opsForValue().set("readCount", 1);
        }
        //这里缓冲区其实就是一个带过期时间的队列
        //先判断一下map当中是否有当前数据
        //bufferTrigger.enqueue(hashmap);
        buffer(id);
        //在Redis中添加ids列表
        //获取当前时间戳
        //保存hash key：readCount field：id value：时间戳
        long timestamp = System.currentTimeMillis();
        redisTemplate.opsForHash().put("readIds" , id , timestamp);
        return 0;
    }

    public void start(){
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            try {
                //创建list列表，用来统一去更新到数据库
                List<Object> list = new ArrayList<>();
                long currentTime = System.currentTimeMillis() / 1000;
                Map<Object, Object> allEntries = redisTemplate.opsForHash().entries("readIds");
                for (Map.Entry<Object, Object> entry : allEntries.entrySet()) {
                    Object field = entry.getKey();
                    Object value = entry.getValue();
                    if (value instanceof Long && ((Long) value) + 10 < currentTime) {
                        // 超时，删除列
                        redisTemplate.opsForHash().delete("readIds", field);
                        list.add(field);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 10, TimeUnit.SECONDS); // 每10秒执行一次
        //更新数据库 list
    }

    LoadingCache<Integer, Integer> cache = CacheBuilder.newBuilder()
            //设置写缓存后8秒钟过期
            .expireAfterWrite(8, TimeUnit.SECONDS)
            //设置缓存移除通知
            .removalListener(new
                RemovalListener<Object, Object>() {
                    public void onRemoval(RemovalNotification<Object, Object>
                                                  notification) {
                        //时间到了或者到一定数量了直接批量更新到mysql当中
                        System.out.println("----"+notification.getKey() + ":" + notification.getCause());
                        Counter counter = new Counter();
                        counter.setId((Integer) notification.getKey());
                        counter.setReadCount((Integer) notification.getValue());
                        updateDB(counter);
                    }
            })
            //build方法可以指定CacheLoader，在缓存不存在时通过CacheLoader的实现自动加载缓存
            .build(new CacheLoader<Integer, Integer>() {
                @Override
                public Integer load(Integer key) throws Exception {
                    return 0;
                }
            });



    /**
     * 缓冲区
     * @param id
     */
    private void buffer(int id){
        //只需要填装数据就行了
        try {
            Integer value = cache.get(id);
            if (value>=200){
                //主动删除缓存
                cache.invalidate(id);
            }else{
                cache.put(id , cache.get(id)+1);
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Async
    public void updateDB(Counter counter){
        //调用数据库
        counterMapper.update(counter);
    }

    /**
     * 更新计数在Redis当中进行更新
     * @param id
     * @param type
     * @return
     */
    @Override
    public int updatePraiseCount(int id, int type) {
        //说明是要点赞的
        if (type==1){

        }else{
            //取消点赞
        }
        return 0;
    }

    @Override
    public int updateTransmitCount(int id, int type) {
        return 0;
    }

    @Override
    public int updateCommentCount(int id, int type) {
        return 0;
    }

    @Override
    public int insert(Counter counter) {
        return 0;
    }

    /**
     * 查询帖子的各种计数信息
     * @param id
     * @return
     */
    @Override
    public Re<Counter> getById(int id) {
        //可以使用redis的hash进行存储，也可以使用字符串进行存储，这里我就使用字符串进行存储了
        String str = (String)redisTemplate.opsForValue().get("counters:" + id);
        //key：counters:xx  value：0:0:0:0
        if(!str.equals("") && str!=null){
            String[] split = str.split(":");
            Counter counter = new Counter();
            counter.setPostId(id);
            counter.setReadCount(Integer.parseInt(split[0]));
            counter.setPraiseCount(Integer.parseInt(split[1]));
            counter.setTransmitCount(Integer.parseInt(split[2]));
            counter.setCommentCount(Integer.parseInt(split[3]));
            return Re.ok(counter);
        }
        return Re.error("查询失败");
        //往缓冲区中写入数据，异步进行写入mysql

    }

    /**
     * 批量查询帖子各种计数
     * @param ids
     * @return
     */
    @Override
    public List<Counter> gets(List<Integer> ids) {
        //查询帖子计数，从redis进行查询
        List<String> list = redisTemplate.opsForValue().multiGet(ids);
        List<Counter> counters = new ArrayList<>();
        int i=0;
        for (String s:list){

            String[] counts = s.split(":");
            Counter counter = new Counter();
            counter.setPostId(ids.get(i));

            for (int n=0; n<counts.length ;n++){
                if (n==0){
                    counter.setReadCount(Integer.parseInt(counts[n]));
                }
                if (n==1){
                    counter.setPraiseCount(Integer.parseInt(counts[n]));
                }
                if (n==2){
                    counter.setTransmitCount(Integer.parseInt(counts[n]));
                }
                if (n==3){
                    counter.setCommentCount(Integer.parseInt(counts[n]));
                }
            }
            counters.add(counter);
            i++;
        }
        return counters;
    }

    /**
     * 查看点赞详情
     * @param cids
     * @param uid
     * @return
     */
    @Override
    public Re<HashMap<Long, Integer>> getPraises(List<Object> cids, int uid) {
        HashMap<Long , Integer> returnMap = new HashMap<>();
        //这里通过前端传入也行，那object就可以改成String类型，这里怕大家忘记给大家去添加一下
        //一次批量查询四条动态
        cids.add("ttl");
        cids.add("mincid");

        //使用hmget查看数据
        List<Long> praises = redisTemplate.opsForHash().multiGet("praise:" + uid, cids);

        //判断缓存是否存在
        if (praises!=null && praises.size()>0){
            //说明缓存存在
            //判断缓存是否需要更新
            Long ttl = praises.get(4);

            //获取当前时间戳
            Long now = System.currentTimeMillis();

            //这里假设我们设置的是6个小时，算时间戳，小于6小时三分之一就是小于2小时
            if ((ttl-now)/3 < 3600000*2){
                //更新时间
                redisTemplate.expire("praise:" + uid , 6 , TimeUnit.HOURS);
                ttl = ttl - (ttl-now) + 3600000*6;
                //更新hash中ttl的过期时间戳
                redisTemplate.opsForHash().put("praise:" + uid , "ttl" , ttl);
            }

            //循环判断cid是否都在结果集当中，-2是因为ttl和mincid不需要进行判断
            for (int i=0 ; i<praises.size()-2 ; i++){
                //如果不=1就判断是否小于mincid
                if (praises.get(i)!=1){

                    //如果大于说明没点赞过
                    if (Long.parseLong((String) cids.get(i))> praises.get(praises.size()-1)){
                        //没点赞过
                        returnMap.put((Long) cids.get(i), 0);
                    }else{
                        //如果小于mincid，就说明是非常老的数据了，就到数据库中进行查询
                        Praise praise = counterMapper.getPraise((Integer) cids.get(i));
                        //说明是历史点赞过的数据
                        if (praise!=null){
                            returnMap.put((Long) cids.get(i), 1);
                        }else{
                            //反之就没点赞过
                            returnMap.put((Long) cids.get(i), 0);
                        }
                    }
                }else{
                    //说明点过赞
                    returnMap.put((Long) cids.get(i), 1);
                }
            }

        }else{
            //如果缓存不存在，需要查询一定时间一定数量的点赞用户信息，时间
            //根据一定规则，这里我直接就查询最近1000条点赞数据，因为一般我刷这种app的时候，平均一天点赞10条左右
            List<String> getPraises = counterMapper.getPraises(uid);
            getPraises.add("ttl");
            getPraises.add("mincid");
            Long now = System.currentTimeMillis();
            Map<String , Long> setMap = new HashMap<>();
            for (String str : getPraises){
                setMap.put(str , 1l);
                if (str.equals("ttl")){
                    setMap.put(str , now+3600000*6);
                }
                if (str.equals("mincid")){
                    setMap.put(str , Long.parseLong(getPraises.get(0)));
                }
            }
            //两步是原子操作，需要注意一下
            redisTemplate.opsForHash().putAll("praise:" + uid , setMap);
            redisTemplate.expire("praise:" + uid , 6 , TimeUnit.HOURS);
        }
        return null;
    }

    /**
     * 保存点赞详情
     * @param pid
     * @param uid
     */
    @Override
    public void insLikeDetails(int pid, int uid) {
        // 加载Lua脚本
        String script = "if redis.call('scard', KEYS[1]) > 1000 then "
                + "redis.call('spop', KEYS[1]) end "
                + "redis.call('sadd', KEYS[1], ARGV[1])";
        // 构造RedisScript对象
        RedisScript<Void> luaScript = new DefaultRedisScript<>(script, Void.class);
        // 执行Lua脚本
        redisTemplate.execute(luaScript, Collections.singletonList("like_details:"+pid), uid);
    }

    /**
     * 分页查询点赞详情
     * @param pid
     * @return
     */
    @Override
    public List<User> likeDetails(int pid) {
        // 设置要查询的key和分页参数
        String key = "like_details:"+pid;
        int page = 1; // 页码从1开始计数
        int pageSize = 10;
        // 计算查询范围
        int startIndex = (page - 1) * pageSize;
        int endIndex = startIndex + pageSize - 1;
        // 从Redis中查询数据
        Set<Integer> uidSet = redisTemplate.opsForZSet().reverseRange(key, startIndex, endIndex);
        if (uidSet != null && !uidSet.isEmpty()) {
            // 如果Redis中有数据，直接返回
        } else {
            // 如果Redis中没有数据，从数据库中查询
            //uidSet = databaseService.getUserUidByPostId(key);
            // 将查询结果保存到Redis中并设置过期时间（单位为秒）
            redisTemplate.opsForZSet().add(key, uidSet);
            redisTemplate.expire(key, Duration.ofDays(60 * 5));
            // 返回查询结果
        }
        //变成String的list通过mget批量查询userId
        List<String> resultList = uidSet.stream()
                .map(id -> "userId:" + id)
                .collect(Collectors.toList());
        List<User> list = redisTemplate.opsForValue().multiGet(resultList);
        //判断list是否存在
        //判断所有user是否有数据
        //如果没有还需要查询一下数据库

        return null;
    }






}
