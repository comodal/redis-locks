package systems.comodal.redis.locks;

import java.util.function.Function;

import com.fabahaba.jedipus.cmds.RESP;

public final class AcquireReply {

  private final String previousOwner;
  private final String currentOwner;
  private final long ttlMillis;

  public static final Function<Object, AcquireReply> ACQUIRE_REPLY_ADAPTER = reply -> {
    final Object[] owners = (Object[]) reply;
    return new AcquireReply(RESP.toString(owners[0]), RESP.toString(owners[1]),
        RESP.longValue(owners[2]));
  };

  AcquireReply(final String previousOwner, final String currentOwner, final long ttlMillis) {
    this.previousOwner = previousOwner;
    this.currentOwner = currentOwner;
    this.ttlMillis = ttlMillis;
  }

  public String getPreviousOwner() {
    return previousOwner;
  }

  public String getCurrentOwner() {
    return currentOwner;
  }

  public long getTTLMillis() {
    return ttlMillis;
  }

  @Override
  public String toString() {
    return "AcquireReply{previousOwner='" + previousOwner + '\''
            + ", currentOwner='" + currentOwner + '\''
            + ", ttlMillis=" + ttlMillis + '}';
  }
}
