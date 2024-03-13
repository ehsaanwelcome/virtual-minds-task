package com.learn.vm;

import com.learn.avro.HourlyRateKey;
import com.learn.avro.HourlyRateValue;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;


@SpringBootApplication
@Configuration
public class StatsApplication implements CommandLineRunner {
	private static final Logger LOG = LoggerFactory.getLogger(StatsApplication.class);

	@Value(value = "${kafka.topic.name}")
	private final String topicName = null;
	@Value(value = "${kafka.stats-topic.name}")
	private final String statsTopicName = null;
	@Value(value = "${kafka.bootstrap-servers}")
	private final String kafkaBootstrapServers = null;
	@Value(value = "${kafka.schema-registery-servers}")
	private final String kafkaSchemaRegiseryServers = null;

	@Autowired
	private ApplicationContext appContext;
	public static void main(String[] args) {
		LOG.info("STARTING THE APPLICATION");
		SpringApplication.run(StatsApplication.class, args);
		LOG.info("APPLICATION FINISHED");
	}

	@Override
	public void run(String... args) throws Exception {
		try {
			//kafka old school props
			var props = new Properties();
			props.put(StreamsConfig.APPLICATION_ID_CONFIG, statsTopicName);
			props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
			props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.Long().getClass().getName());
			props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class);
			props.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, kafkaSchemaRegiseryServers);
			props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
			props.put(StreamsConfig.STATE_DIR_CONFIG, Files.createTempDirectory("kafka").toFile().getAbsolutePath());

			//avro serdes to produce avro key and value
			final Map<String, String> serdeConfig = Collections.singletonMap(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, kafkaSchemaRegiseryServers);
			final Serde<HourlyRateKey> keySerde = new SpecificAvroSerde<>();
			keySerde.configure(serdeConfig, true);
			final Serde<HourlyRateValue> valueSerde = new SpecificAvroSerde<>();
			valueSerde.configure(serdeConfig, false);

			final StreamsBuilder streamBuilder = new StreamsBuilder();

			//read events e.g. customerid and request validity (boolean)
			KStream<Long, Boolean> events = streamBuilder.stream(topicName, Consumed.with(Serdes.Long(), Serdes.Boolean()));

			//create one hour window of events and aggregate valid and invalid requests
			KTable<Windowed<Long>, HourlyRateValue> stats = events
					.map((k, v) -> {
						var value = HourlyRateValue.newBuilder().setRequestCount(v ? 1 : 0).setInvalidCount(!v ? 1 : 0).build();
						return new KeyValue<Long, HourlyRateValue>(k, value);
					})
					.groupByKey()
					.windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofHours(1)))
					.aggregate(() -> new HourlyRateValue(0L, 0L),
							(k, v, agg) -> {
								agg.setRequestCount(agg.getRequestCount() + v.getRequestCount());
								agg.setInvalidCount(agg.getInvalidCount() + v.getInvalidCount());
								return agg;
							}, Materialized.with(Serdes.Long(), valueSerde)
					);

			//dump winowed data to new topic which contain customer f, time, valid and invalid requests
			stats
					.toStream()
					.filter((k, v) -> v.getRequestCount() > 0 || v.getInvalidCount() > 0)
					.map((k, v) -> {
						var key = HourlyRateKey.newBuilder().setCustomerId(k.key()).setTime(k.window().startTime().toEpochMilli()).build();
						return new KeyValue<>(key, v);
					})
					.peek((k, v) -> LOG.info("{} {}", k, v))
					.to(statsTopicName, Produced.with(keySerde, valueSerde));


			var kafkaStream = new KafkaStreams(streamBuilder.build(), props);
			kafkaStream.start();
			Runtime.getRuntime().addShutdownHook(new Thread(kafkaStream::close));
		} catch (Exception exception) {
			LOG.error(exception.getMessage());
		}
	}
}
