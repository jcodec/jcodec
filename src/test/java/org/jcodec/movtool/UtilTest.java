package org.jcodec.movtool;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.jcodec.common.model.Rational;
import org.jcodec.containers.mp4.boxes.Edit;
import org.junit.Test;

public class UtilTest {
    @Test
    public void testEditsOnEdits0() {
       List<Edit> lower = new ArrayList<Edit>();
       List<Edit> higher = new ArrayList<Edit>();
       
       lower.add(new Edit(9000, 0, 1.0f));
       lower.add(new Edit(10000, 9000, 1.0f));
       
       higher.add(new Edit(8000, 1000, 1.0f));
       higher.add(new Edit(9000, 10000, 1.0f));
       
       List<Edit> out = Util.editsOnEdits(Rational.R(1000, 1000), lower, higher);

       for (int i = 0; i < 2; i++) {
         assertEquals(higher.get(i).getMediaTime(), out.get(i).getMediaTime());
         assertEquals(higher.get(i).getDuration(), out.get(i).getDuration());
       }
    }
    
    @Test
    public void testEditsOnEdits1() {
       List<Edit> lower = new ArrayList<Edit>();
       List<Edit> higher = new ArrayList<Edit>();
       
       lower.add(new Edit(9000, 0, 1.0f));
       
       higher.add(new Edit(9000, 0, 1.0f));
       
       List<Edit> out = Util.editsOnEdits(Rational.R(1000, 1000), lower, higher);

       for (int i = 0; i < 1; i++) {
         assertEquals(higher.get(i).getMediaTime(), out.get(i).getMediaTime());
         assertEquals(higher.get(i).getDuration(), out.get(i).getDuration());
       }
    }
    
    @Test
    public void testEditsOnEdits2() {
        List<Edit> lower = new ArrayList<Edit>();
        List<Edit> higher = new ArrayList<Edit>();
        
        lower.add(new Edit(3000, 4000, 1.0f));
        lower.add(new Edit(4000, 14000, 1.0f));
        lower.add(new Edit(4000, 24000, 1.0f));
        
        higher.add(new Edit(3000, 2000, 1.0f));
        higher.add(new Edit(3000, 6000, 1.0f));
        
        List<Edit> out = Util.editsOnEdits(Rational.R(1000, 1000), lower, higher);
        
        List<Edit> expect = new ArrayList<Edit>();
        expect.add(new Edit(1000, 6000, 1.0f));
        expect.add(new Edit(2000, 14000, 1.0f));
        expect.add(new Edit(1000, 17000, 1.0f));
        expect.add(new Edit(2000, 24000, 1.0f));

        for (int i = 0; i < 4; i++) {
          assertEquals(expect.get(i).getMediaTime(), out.get(i).getMediaTime());
          assertEquals(expect.get(i).getDuration(), out.get(i).getDuration());
        }
    }
    
    @Test
    public void testEditsOnEdits3() {
        List<Edit> lower = new ArrayList<Edit>();
        List<Edit> higher = new ArrayList<Edit>();
        
        lower.add(new Edit(3000, 4000, 1.0f));
        lower.add(new Edit(4000, 24000, 1.0f));
        lower.add(new Edit(4000, 14000, 1.0f));
        
        higher.add(new Edit(3000, 2000, 1.0f));
        higher.add(new Edit(3000, 6000, 1.0f));
        
        List<Edit> out = Util.editsOnEdits(Rational.R(1000, 1000), lower, higher);
        
        List<Edit> expect = new ArrayList<Edit>();
        expect.add(new Edit(1000, 6000, 1.0f));
        expect.add(new Edit(2000, 24000, 1.0f));
        expect.add(new Edit(1000, 27000, 1.0f));
        expect.add(new Edit(2000, 14000, 1.0f));

        for (int i = 0; i < 4; i++) {
          assertEquals(expect.get(i).getMediaTime(), out.get(i).getMediaTime());
          assertEquals(expect.get(i).getDuration(), out.get(i).getDuration());
        }
    }
    
    @Test
    public void testEditsOnEdits4() {
        List<Edit> lower = new ArrayList<Edit>();
        List<Edit> higher = new ArrayList<Edit>();
        
        lower.add(new Edit(3000, 4000, 1.0f));
        lower.add(new Edit(4000, 24000, 1.0f));
        lower.add(new Edit(4000, 14000, 1.0f));
        
        higher.add(new Edit(3000, 6000, 1.0f));
        higher.add(new Edit(3000, 2000, 1.0f));
        
        List<Edit> out = Util.editsOnEdits(Rational.R(1000, 1000), lower, higher);
        
        List<Edit> expect = new ArrayList<Edit>();
        expect.add(new Edit(1000, 27000, 1.0f));
        expect.add(new Edit(2000, 14000, 1.0f));
        expect.add(new Edit(1000, 6000, 1.0f));
        expect.add(new Edit(2000, 24000, 1.0f));
        
        for (int i = 0; i < 4; i++) {
          assertEquals(expect.get(i).getMediaTime(), out.get(i).getMediaTime());
          assertEquals(expect.get(i).getDuration(), out.get(i).getDuration());
        }
    }
}
