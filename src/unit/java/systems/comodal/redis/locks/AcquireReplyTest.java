package systems.comodal.redis.locks;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AcquireReplyTest {

  @Test
  public void testToString() {
    AcquireReply testReply = new AcquireReply(null, "owner", 1L);
    assertNull(testReply.getPreviousOwner());
    assertEquals("owner", testReply.getCurrentOwner());
    assertEquals(1L, testReply.getTTLMillis());
    assertEquals("AcquireReply{previousOwner=null, currentOwner='owner', ttlMillis=1}", testReply.toString());

    testReply = new AcquireReply("owner", "newOwner", 1L);
    assertEquals("owner", testReply.getPreviousOwner());
    assertEquals("newOwner", testReply.getCurrentOwner());
    assertEquals(1L, testReply.getTTLMillis());
    assertEquals("AcquireReply{previousOwner='owner', currentOwner='newOwner', ttlMillis=1}",
        testReply.toString());
  }
}
