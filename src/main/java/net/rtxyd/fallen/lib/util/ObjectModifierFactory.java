package net.rtxyd.fallen.lib.util;

import sun.reflect.ReflectionFactory;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class must be thread-safe.
 */
public class ObjectModifierFactory {

    // cache instantiators
    private final Map<Class<?>, Constructor<?>> instantiatorCache = new ConcurrentHashMap<>();
    // cache fields (including superclasses fields) for non-record class
    private final Map<Class<?>, Field[]> fieldsCache = new ConcurrentHashMap<>();
    // cache record components for construct a new instance with canonical constructor.
    private final Map<Class<?>, RecordComponent[]> recordComponentsCache = new ConcurrentHashMap<>();
    // cache accessors
    private final Map<Class<?>, Method[]> recordAccessorCache = new ConcurrentHashMap<>();
    // cache accessors
    private final Map<Class<?>, boolean[]> fieldFilterCache = new ConcurrentHashMap<>();
    // add to blackList if you can't modify
    private final Set<Class<?>> blackList = ConcurrentHashMap.newKeySet();
    // filter field name
    private final Set<String> fieldNameFilter = ConcurrentHashMap.newKeySet();

    public RecordComponent[] collectRecordComponents(Class<?> clazz) {
        return clazz.getRecordComponents();
    }

    public void addToBlackList(Class<?> clazz) {
        blackList.add(clazz);
    }

    public boolean isInBlackList(Class<?> clazz) {
        return blackList.contains(clazz);
    }

    public void addFiledNameFilter(String name) {
        fieldNameFilter.add(name);
    }

    /**
     * Input a record-like class object, multiply all number fields,
     * Output a new object with the same class, while all numbers are multiplied.
     * Only numbers are changed, for non-number fields, it copies them to the new object.
     * @param original The object need to be parsed
     * @param multiplied The multiplied times
     * @return New object, while same class, only number fields are multiplied.
     * @param <T> The class
     */
    public <T> T copyAndModifyNumbers(T original, float multiplied) {
        if (original == null) return null;
        Class<?> clazz = original.getClass();
        if (blackList.contains(clazz)) return original;
        try {
            if (clazz.isRecord()) {
                return copyAndModifyNumbersRecord(clazz, original, multiplied);
            }
            return copyAndModifyNumbersFallback(clazz, original, multiplied);
        } catch (Exception ignore) {
            blackList.add(original.getClass());
            return original;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T copyAndModifyNumbersRecord(Class<?> clazz, T original, float multiplied) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        RecordComponent[] components = recordComponentsCache.computeIfAbsent(clazz, this::collectRecordComponents);
        Method[] accessors = recordAccessorCache.computeIfAbsent(clazz, c -> {
            Method[] accs = new Method[components.length];
            for (int i = 0; i < components.length; i++) {
                Method acc = components[i].getAccessor();
                acc.setAccessible(true);
                accs[i] = acc;
            }
            return accs;
        });
        Constructor<?> instantiator = instantiatorCache.computeIfAbsent(clazz, c -> {
            try {
                Class<?>[] argTypes = new Class<?>[components.length];
                for (int i = 0; i < components.length; i++) {
                    argTypes[i] = components[i].getType();
                }
                Constructor<?> constructor = c.getDeclaredConstructor(argTypes);
                constructor.setAccessible(true);
                return constructor;
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        });
        Object[] parameters = new Object[accessors.length];
        boolean[] filterCache = fieldFilterCache.computeIfAbsent(clazz, c -> {
            boolean[] isOK = new boolean[components.length];
            Arrays.fill(isOK, true);
            for (int i = 0; i < components.length; i++) {
                for (String filter : fieldNameFilter) {
                    String compName = components[i].getName().toLowerCase();
                    if (compName.startsWith(filter) || compName.endsWith(filter)) {
                        isOK[i] = false;
                        break;
                    }
                }
            }
            return isOK;
        });
        for (int i = 0; i < parameters.length; i++) {
            Method acc = accessors[i];
            Object val = acc.invoke(original);
            if (val instanceof Number num && filterCache[i]) {
                Number newVal = modifyNumber(num, multiplied);
                parameters[i] = newVal;
            } else {
                parameters[i] = val;
            }
        }
        return (T) instantiator.newInstance(parameters);
    }

    @SuppressWarnings("unchecked")
    private <T> T copyAndModifyNumbersFallback(Class<?> clazz, T original, float multiplied) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        Constructor<?> instantiator = instantiatorCache.computeIfAbsent(clazz, this:: createInstantiator);
        T copy = (T) instantiator.newInstance();

        Field[] fields = fieldsCache.computeIfAbsent(clazz, this::collectAllFilteredFields);

        for (Field field : fields) {
            field.setAccessible(true);
            Class<?> t = field.getType();

            if (t.isPrimitive()) {
                if (t == int.class) {
                    int v = field.getInt(original);
                    field.setInt(copy, (int)(v * multiplied));
                } else if (t == long.class) {
                    long v = field.getLong(original);
                    field.setLong(copy, (long) (v * multiplied));
                } else if (t == double.class) {
                    double v = field.getDouble(original);
                    field.setDouble(copy, (double) (v * multiplied));
                } else if (t == float.class) {
                    float v = field.getFloat(original);
                    field.setFloat(copy, (float) (v * multiplied));
                } else if (t == short.class) {
                    short v = field.getShort(original);
                    field.setShort(copy, (short) (v * multiplied));
                } else if (t == byte.class) {
                    byte v = field.getByte(original);
                    field.setByte(copy, (byte) (v * multiplied));
                } else {
                    // boolean, char or others
                    Object val = field.get(original);
                    field.set(copy, val);
                }
            } else {
                // not primitive
                Object val = field.get(original);
                if (val instanceof Number num) {
                    Number result = (Number) modifyNumber(num, multiplied);
                    field.set(copy, result);
                } else {
                    // shadow other objects
                    field.set(copy, val);
                }
            }
        }
        return copy;
    }

    // create a constructor without parameter
    public Constructor<?> createInstantiator(Class<?> clazz) {
        try {
            Constructor<?> objDef = Object.class.getDeclaredConstructor();
            Constructor<?> cons = ReflectionFactory.getReflectionFactory().newConstructorForSerialization(clazz, objDef);
            cons.setAccessible(true);
            return cons;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create instantiator for " + clazz, e);
        }
    }

    // collect all declared fields from class and superclasses (excluding Object)
    public Field[] collectAllFilteredFields(Class<?> clazz) {
        List<Field> list = new ArrayList<>();
        Class<?> c = clazz;
        while (c != null && c != Object.class) {
            Field[] declared = c.getDeclaredFields();
            for (Field field : declared) {
                boolean shouldAdd = true;
                String name = field.getName();
                for (String filter : fieldNameFilter) {
                    if (name.contains(filter)) {
                        Class<?> type = field.getType();
                        if (type.isPrimitive() && type != char.class && type != boolean.class || Number.class.isAssignableFrom(type)) {
                            shouldAdd = false;
                        }
                    }
                }
                if (shouldAdd) {
                    list.add(field);
                }
            }
            c = c.getSuperclass();
        }
        return list.toArray(new Field[0]);
    }

    public static Number modifyNumber(Number n, float multiplied) {
        // common
        if (n instanceof Float f) return (float) (f * multiplied);
        if (n instanceof Integer i) return (int) (i * multiplied);
        if (n instanceof Double d) return (double) (d * multiplied);
        // less common
        if (n instanceof Long l) return (long) (l * multiplied);
        if (n instanceof Short s) return (short) (s * multiplied);
        if (n instanceof Byte b) return (byte) (b * multiplied);
        // BigInteger, BigDecimal ignore
        return n;
    }
}