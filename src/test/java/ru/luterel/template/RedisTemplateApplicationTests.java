package ru.luterel.template;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.luterel.template.api.dto.PostResponse;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class RedisTemplateApplicationTests {

	@Container
	static PostgreSQLContainer postgres =
			new PostgreSQLContainer("postgres:16");

	@Container
	static GenericContainer<?> redis =
			new GenericContainer<>("redis:8.0-alpine")
					.withExposedPorts(6379);

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.data.redis.host", redis::getHost);
		registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
		registry.add("server.port", () -> 0);
	}

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void getCachedPostInRedis() throws IOException, InterruptedException {
		Map<String, Object> request = Map.of(
				"title", "First post",
				"body", "Hello",
				"status", "DRAFT",
				"tags", "java,spring,redis"
		);

		ResponseEntity<PostResponse> createResponse =
				restTemplate.postForEntity("/posts", request, PostResponse.class);

		assertThat(createResponse.getStatusCode().value()).isEqualTo(201);
		assertThat(createResponse.getBody()).isNotNull();
		assertThat(createResponse.getBody().id()).isNotNull();

		Long postId = createResponse.getBody().id();
		String cacheKey = "post:id:" + postId;

		var keyBeforeGet = redis.execInContainer("redis-cli", "EXISTS", cacheKey);
		assertThat(keyBeforeGet.getStdout().trim()).isEqualTo("0");

		ResponseEntity<PostResponse> firstGetResponse =
				restTemplate.getForEntity("/posts/" + postId, PostResponse.class);

		assertThat(firstGetResponse.getStatusCode().value()).isEqualTo(200);
		assertThat(firstGetResponse.getBody()).isNotNull();
		assertThat(firstGetResponse.getBody().id()).isEqualTo(postId);
		assertThat(firstGetResponse.getBody().title()).isEqualTo("First post");

		var keyAfterFirstGet = redis.execInContainer("redis-cli", "EXISTS", cacheKey);
		assertThat(keyAfterFirstGet.getStdout().trim()).isEqualTo("1");

		ResponseEntity<PostResponse> secondGetResponse =
				restTemplate.getForEntity("/posts/" + postId, PostResponse.class);

		assertThat(secondGetResponse.getStatusCode().value()).isEqualTo(200);
		assertThat(secondGetResponse.getBody()).isNotNull();
		assertThat(secondGetResponse.getBody().id()).isEqualTo(postId);
		assertThat(secondGetResponse.getBody().title()).isEqualTo("First post");
		assertThat(secondGetResponse.getBody().body()).isEqualTo("Hello");
		assertThat(secondGetResponse.getBody().status()).isEqualTo("DRAFT");
		assertThat(secondGetResponse.getBody().tags()).isEqualTo("java,spring,redis");
	}

	@Test
	void updateInvalidatesCacheAndNextGetRebuildsIt() throws Exception {
		Map<String, Object> createRequest = Map.of(
				"title", "First post",
				"body", "Hello",
				"status", "DRAFT",
				"tags", "java,spring,redis"
		);

		ResponseEntity<PostResponse> createResponse =
				restTemplate.postForEntity("/posts", createRequest, PostResponse.class);

		assertThat(createResponse.getStatusCode().value()).isEqualTo(201);
		assertThat(createResponse.getBody()).isNotNull();
		assertThat(createResponse.getBody().id()).isNotNull();

		Long postId = createResponse.getBody().id();
		String cacheKey = "post:id:" + postId;

		ResponseEntity<PostResponse> firstGetResponse =
				restTemplate.getForEntity("/posts/" + postId, PostResponse.class);

		assertThat(firstGetResponse.getStatusCode().value()).isEqualTo(200);

		var keyAfterWarmup = redis.execInContainer("redis-cli", "EXISTS", cacheKey);
		assertThat(keyAfterWarmup.getStdout().trim()).isEqualTo("1");

		Map<String, Object> updateRequest = Map.of(
				"title", "Updated post",
				"body", "Updated body",
				"status", "PUBLISHED",
				"tags", "updated,redis"
		);

		restTemplate.put("/posts/" + postId, updateRequest);

		var keyAfterUpdate = redis.execInContainer("redis-cli", "EXISTS", cacheKey);
		assertThat(keyAfterUpdate.getStdout().trim()).isEqualTo("0");

		ResponseEntity<PostResponse> getAfterUpdateResponse =
				restTemplate.getForEntity("/posts/" + postId, PostResponse.class);

		assertThat(getAfterUpdateResponse.getStatusCode().value()).isEqualTo(200);
		assertThat(getAfterUpdateResponse.getBody()).isNotNull();
		assertThat(getAfterUpdateResponse.getBody().id()).isEqualTo(postId);
		assertThat(getAfterUpdateResponse.getBody().title()).isEqualTo("Updated post");
		assertThat(getAfterUpdateResponse.getBody().body()).isEqualTo("Updated body");
		assertThat(getAfterUpdateResponse.getBody().status()).isEqualTo("PUBLISHED");
		assertThat(getAfterUpdateResponse.getBody().tags()).isEqualTo("updated,redis");

		var keyAfterSecondGet = redis.execInContainer("redis-cli", "EXISTS", cacheKey);
		assertThat(keyAfterSecondGet.getStdout().trim()).isEqualTo("1");
	}

	@Test
	void deleteRemovesCacheAndPostBecomesUnavailable() throws Exception {
		Map<String, Object> createRequest = Map.of(
				"title", "First post",
				"body", "Hello",
				"status", "DRAFT",
				"tags", "java,spring,redis"
		);

		ResponseEntity<PostResponse> createResponse =
				restTemplate.postForEntity("/posts", createRequest, PostResponse.class);

		assertThat(createResponse.getStatusCode().value()).isEqualTo(201);
		assertThat(createResponse.getBody()).isNotNull();
		assertThat(createResponse.getBody().id()).isNotNull();

		Long postId = createResponse.getBody().id();
		String cacheKey = "post:id:" + postId;

		ResponseEntity<PostResponse> firstGetResponse =
				restTemplate.getForEntity("/posts/" + postId, PostResponse.class);

		assertThat(firstGetResponse.getStatusCode().value()).isEqualTo(200);

		var keyAfterWarmup = redis.execInContainer("redis-cli", "EXISTS", cacheKey);
		assertThat(keyAfterWarmup.getStdout().trim()).isEqualTo("1");

		restTemplate.delete("/posts/" + postId);

		var keyAfterDelete = redis.execInContainer("redis-cli", "EXISTS", cacheKey);
		assertThat(keyAfterDelete.getStdout().trim()).isEqualTo("0");

		ResponseEntity<String> getAfterDeleteResponse =
				restTemplate.getForEntity("/posts/" + postId, String.class);

		assertThat(getAfterDeleteResponse.getStatusCode().value()).isEqualTo(404);
	}
}
