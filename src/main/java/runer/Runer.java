package runer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer011;
import org.apache.flink.streaming.connectors.redis.RedisSink;
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisConfigBase;
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisPoolConfig;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommand;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommandDescription;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisMapper;

import java.util.Properties;

public class Runer {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        //载入数据
//        DataStream<String> sourceStream = env.readTextFile("./data/input/log", "UTF-8");
        String topic = "event_topic";
        Properties prop = new Properties();
        prop.put("bootstrap.servers","47.113.205.225:9092");//47.113.205.225  172.16.10.193
        prop.put("zookeeper.connect","47.113.205.225:2181");
        prop.put("group.id","flink_consumer");
        DataStream<String> sourceStream = env.addSource(new FlinkKafkaConsumer011<String>(topic,new SimpleStringSchema(),prop));

        //转换计算
        DataStream<String> sinkStream = sourceStream;
        sourceStream.print();

        //数据下沉
        FlinkJedisPoolConfig jedisConf = new FlinkJedisPoolConfig.Builder()
                .setHost("47.113.205.225")
                .setPort(6379)
                .build();
        sinkStream.addSink(new RedisSink<String>(jedisConf, new myRedisMapper()));

        //执行
        env.execute();
    }

    private static class myRedisMapper implements RedisMapper<String>{
        //存到redis的命令
        @Override
        public RedisCommandDescription getCommandDescription() {
            return new RedisCommandDescription(RedisCommand.HSET,"noah_redis");
        }

        @Override
        public String getKeyFromData(String s) {
            JSONObject jsonObject = JSON.parseObject(s);
            return jsonObject.getString("distinct_id");
        }

        @Override
        public String getValueFromData(String s) {
            JSONObject jsonObject = JSON.parseObject(s);
            return jsonObject.getString("time");
        }
    }
}
