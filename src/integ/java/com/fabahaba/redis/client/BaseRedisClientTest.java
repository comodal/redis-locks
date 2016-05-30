package com.fabahaba.redis.client;

import java.util.Optional;

import org.junit.After;
import org.junit.Before;

import com.fabahaba.jedipus.client.RedisClient;
import com.fabahaba.jedipus.cluster.Node;
import com.fabahaba.jedipus.cmds.Cmds;
import com.fabahaba.jedipus.primitive.RedisClientFactory;

public class BaseRedisClientTest {

  protected BaseRedisClientTest() {}

  public static final int REDIS_PORT = Optional
      .ofNullable(System.getProperty("redislocks.redis.port")).map(Integer::parseInt).orElse(9736);

  public static final Node DEFAULT_NODE = Node.create("localhost", REDIS_PORT);

  public static final RedisClientFactory.Builder DEFAULT_CLIENT_FACTORY_BUILDER =
      RedisClientFactory.startBuilding();

  protected RedisClient client = null;

  @Before
  public void before() {
    client = DEFAULT_CLIENT_FACTORY_BUILDER.create(DEFAULT_NODE);
    client.sendCmd(Cmds.FLUSHALL.raw());
  }

  @After
  public void after() {

    if (client == null || client.isBroken()) {
      return;
    }

    client.close();
  }
}
