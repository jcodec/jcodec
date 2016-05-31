package org.jcodec.common.tools;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ToJSONTest {

    static class Cl1 {
        private List<Cl2> arr;

        public Cl1() {
            this.arr = new ArrayList<Cl2>();
        }

        public void addCl2(Cl2 inst) {
            arr.add(inst);
        }

        public List<Cl2> getArr() {
            return arr;
        }
    }

    static class Cl2 {
        private Cl1 owner;

        public Cl2(Cl1 owner) {
            this.owner = owner;
        }

        public Cl1 getOwner() {
            return owner;
        }
    }

    @Test(timeout = 1000)
    public void testCycle() {
        Cl1 cl1 = new Cl1();
        cl1.addCl2(new Cl2(cl1));
        cl1.addCl2(new Cl2(cl1));
        cl1.addCl2(new Cl2(cl1));
        cl1.addCl2(new Cl2(cl1));
        cl1.addCl2(new Cl2(cl1));

        Assert.assertEquals("{\"arr\":[" + "{\"owner\":null,},"
                + "{\"owner\":null,}," + "{\"owner\":null,},"
                + "{\"owner\":null,}," + "{\"owner\":null,}" + "],}",
                ToJSON.toJSON(cl1));
    }

    @Test
    public void testMap() {
//@formatter:off
        Map<String, String[]> europe = new LinkedHashMap<String, String[]>();
        europe.put("ukraine", new String[] {"Kyiv", "Lviv", "Odessa", "Kharkiv"});
        europe.put("russia", new String[] {"Moscow", "St. Petersburg", "Tver", "Novosibirsk"});
        
        Map<String, String[]> asia = new LinkedHashMap<String, String[]>();
        asia.put("china", new String[] {"Beijing", "Shanghai", "Chongqing", "Tianjin"});
        asia.put("japan", new String[] {"Tokyo", "Kyoto", "Osaka", "Hakone"});
        asia.put("korea", new String[] {"Seoul", "Busan", "Daegu", "Daejon"});

        Map<String, Map<String, String[]>> map = new LinkedHashMap<String, Map<String, String[]>>();
        map.put("europe",  europe);
        map.put("asia", asia);
//@formatter:on
        Assert.assertEquals("{" + "\"europe\":" + "{" + "\"ukraine\":[\"Kyiv\",\"Lviv\",\"Odessa\",\"Kharkiv\"],"
                + "\"russia\":[\"Moscow\",\"St. Petersburg\",\"Tver\",\"Novosibirsk\"]" + "}," + "\"asia\":" + "{"
                + "\"china\":[\"Beijing\",\"Shanghai\",\"Chongqing\",\"Tianjin\"],"
                + "\"japan\":[\"Tokyo\",\"Kyoto\",\"Osaka\",\"Hakone\"],"
                + "\"korea\":[\"Seoul\",\"Busan\",\"Daegu\",\"Daejon\"]" + "}" + "}", ToJSON.toJSON(map));
    }
}