local size = redis.call('get' , KEYS[1])
if(tonumber(size)>=200) then
    redis.call('rpop' , KEYS[1])
end
redis.call('lpush' , KEYS[1] , ARGV[1])
return 0
