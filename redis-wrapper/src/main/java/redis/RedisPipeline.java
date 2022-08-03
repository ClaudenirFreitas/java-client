package redis;

import pluggable.Result;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;

import java.util.List;
import java.util.stream.Collectors;

public class RedisPipeline implements pluggable.Pipeline {
    private Pipeline _pipelined;
    private final String _prefix;


    public RedisPipeline(JedisPool jedisPool, String prefix) {
        _prefix = prefix;
        try (Jedis jedis = jedisPool.getResource()) {
            _pipelined = jedis.pipelined();
        } catch (Exception ex) {
            System.out.println("err " + ex.getMessage());
        }
    }

    /* package private */ String buildKeyWithPrefix(String key) {
        if (!key.startsWith(_prefix)) {
            key = String.format("%s.%s", _prefix, key);
        }

        return key;
    }

    @Override
    public void hIncrement(String key, String field, long value) {
        _pipelined.hincrBy(buildKeyWithPrefix(key), field, value);
    }

    @Override
    public List<Result> exec() throws Exception {
        try{
            List<Object> executionResult = _pipelined.syncAndReturnAll();
            return executionResult.stream().map(i -> new Result(i)).collect(Collectors.toList());
        } catch (Exception e) {
            throw new RedisException(e.getMessage());
        }
    }
}
