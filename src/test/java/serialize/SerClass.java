package serialize;

import serialize.sermod.GetterIntf;

import java.io.IOException;
import java.io.Serializable;

public class SerClass implements Serializable {

    private final GetterIntf<Integer> impInteger ;
    private final GetterIntf<String> impString ;
    private int i;

    public SerClass() {
        this.impInteger = null;
        this.impString = null;
    }

    public SerClass(GetterIntf<Integer> impInteger, GetterIntf<String> impString) {
        this.impInteger = impInteger;
        this.impString = impString;
    }

    public Integer getInnerInteger() {
        if(impInteger != null) {
            return impInteger.get();
        }
        return 0;
    }

    public String getInnerString() {
        if(impString != null) {
            return impString.get();
        }
        return "--";
    }


    public static void main(String[] args) throws IOException {
//        SerClass serClass = new SerClass(new GetterImp<>(), new GetterImp<>());
//        ObjectMapper mapper = new ObjectMapper();
//        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
//        String json = mapper.writeValueAsString(serClass);
//        System.out.println(json);
//
//        SerClass serClass2 = mapper.readValue(json, SerClass.class);
//        System.out.println(serClass2);


    }

}
