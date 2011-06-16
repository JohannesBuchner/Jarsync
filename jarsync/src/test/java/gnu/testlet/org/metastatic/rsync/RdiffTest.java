package gnu.testlet.org.metastatic.rsync;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.metastatic.rsync.ChecksumPair;
import org.metastatic.rsync.Rdiff;

public class RdiffTest
{
  private static final Logger log = Logger.getLogger(RdiffTest.class);

  private File folder = new File("target/test-classes/rdifftest");

  @Test
  public void testRdiff() throws NoSuchAlgorithmException, FileNotFoundException, IOException
  {
    for (int i = 1; i < 10; i++)
      {
        String prefix = "mut." + i;
        File mutated = new File(folder, prefix);
        log.debug("Testing " + mutated.getAbsolutePath());
        File output = new File(folder, prefix + ".output");
        File signature= new File(folder, prefix + ".signature");
        File newdelta = new File(folder, prefix + ".newdelta");
        File basis = new File(folder, "basis");
        
        // client does this first
        Rdiff clientRdiff = new Rdiff();
        clientRdiff.makeSignatures(new FileInputStream(mutated), new FileOutputStream(signature));
        
        // server gets signature file, and does this now
        Rdiff serverRdiff = new Rdiff();
        List<ChecksumPair> sigs = serverRdiff.readSignatures(new FileInputStream(signature));
        serverRdiff.makeDeltas(sigs, new FileInputStream(basis), new FileOutputStream(newdelta));

        // finally, client patches his file
        clientRdiff.rebuildFile(mutated, new FileInputStream(newdelta), new FileOutputStream(output));
        
        checkSame(output, basis);
      }
  }

  private void checkSame(File a, File b) throws IOException
  {
    FileInputStream as = new FileInputStream(a);
    FileInputStream bs = new FileInputStream(b);
    while(true) {
      int ai = as.read();
      int bi = bs.read();
      Assert.assertEquals(ai, bi);
      if (ai == -1)
        return;
    }
  }
}
