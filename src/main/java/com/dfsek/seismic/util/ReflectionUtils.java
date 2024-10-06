package com.dfsek.seismic.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;


public class ReflectionUtils {
    public static ConcurrentHashMap<String, Class> reflectedClasses = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<ClassField, Field> reflectedFields = new ConcurrentHashMap<>();

    /**
     * Retrieves the class object associated with the given class name.
     *
     * @param className the name of the class
     * @return the class object for the given class name
     */
    public static Class getClass(String className) {
        return reflectedClasses.computeIfAbsent(className, ReflectionUtils::getReflectedClass);
    }

    /**
     * Retrieves the field object for the given class and field name.
     *
     * @param clss the class containing the field
     * @param fild the name of the field
     * @return the field object for the given class and field name
     */
    public static Field getField(Class clss, String fild) {
        return reflectedFields.computeIfAbsent(new ClassField(clss, fild), ReflectionUtils::getReflectedField);
    }

    /**
     * Sets the specified field to be accessible, bypassing Java access control checks.
     *
     * @param fild the field to set to public
     */
    public static void setFieldToPublic(Field fild) {
        setAccessibleObjectToPublic(fild);
    }

    /**
     * Sets the specified method to be accessible, bypassing Java access control checks.
     *
     * @param mthod the method to set to public
     */
    public static void setMethodToPublic(Method mthod) {
        setAccessibleObjectToPublic(mthod);
    }

    private static void setAccessibleObjectToPublic(AccessibleObject obj) {
        try {
            obj.setAccessible(true);
        } catch (SecurityException se) {
            try {
                Class.forName("java.security.AccessController");
                AccessControllerUtils.doPrivileged(() -> {
                    obj.setAccessible(true);
                    return null;
                });
            } catch (ClassNotFoundException e) {
                if (UnsafeUtils.canUseUnsafe) {
                    UnsafeUtils.putFieldBoolean(obj, getField(obj.getClass(), "override"), true);
                }
            }
        }
    }

    private static Field getReflectedField(ClassField clssfild) {
        try {
            return clssfild.clss.getField(clssfild.fild());
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Class getReflectedClass(String className) {
        try {
            Class classObj;
            int $loc = className.indexOf("$");
            if ($loc > -1) {
                classObj = getNestedClass(Class.forName(className.substring(0, $loc)), className.substring($loc + 1));
            } else {
                classObj = Class.forName(className);
            }
            assert classObj != null;
            return classObj;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Class getNestedClass(Class upperClass, String nestedClassName) {
        Class[] classObjArr = upperClass.getDeclaredClasses();
        for (Class classArrObj : classObjArr) {
            if (classArrObj.getName().equals(upperClass.getName() + "$" + nestedClassName)) {
                return classArrObj;
            }
        }
        return null;
    }

    /**
     * Executes the given operation if the specified annotation is present on the element.
     *
     * @param <T> the type of the annotation
     * @param element the annotated element
     * @param annotation the annotation class
     * @param operation the operation to execute if the annotation is present
     */
    public static <T extends Annotation> void ifAnnotationPresent(AnnotatedElement element, Class<? extends T> annotation,
                                                                  Consumer<T> operation) {
        T a = element.getAnnotation(annotation);
        if(a != null) operation.accept(a);
    }

    /**
     * Returns the raw type of the given type.
     *
     * @param type the type to get the raw type of
     * @return the raw type of the given type
     */
    public static Class<?> getRawType(Type type) {
        if(type instanceof Class<?>) {
            return (Class<?>) type;
        } else if(type instanceof ParameterizedType parameterizedType) {
            Type rawType = parameterizedType.getRawType();
            return (Class<?>) rawType;
        } else if(type instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) type).getGenericComponentType();
            return Array.newInstance(getRawType(componentType), 0).getClass();
        } else if(type instanceof TypeVariable) {
            return Object.class;
        } else if(type instanceof WildcardType) {
            return getRawType(((WildcardType) type).getUpperBounds()[0]);
        } else {
            String className = type == null ? "null" : type.getClass().getName();
            throw new IllegalArgumentException("Expected a Class, ParameterizedType, or "
                                               + "GenericArrayType, but <" + type + "> is of type " + className);
        }
    }

    /**
     * Converts the given type to a string representation.
     *
     * @param type the type to convert to a string
     * @return the string representation of the type
     */
    public static String typeToString(Type type) {
        return type instanceof Class ? ((Class<?>) type).getName() : type.toString();
    }

    /**
     * Compares two types for equality.
     *
     * @param a the first type to compare
     * @param b the second type to compare
     * @return true if the types are equal, false otherwise
     */
    public static boolean equals(Type a, Type b) {
        if(a == b) {
            return true;
        } else if(a instanceof Class) {
            return a.equals(b);
        } else if(a instanceof ParameterizedType pa) {
            if(!(b instanceof ParameterizedType pb)) {
                return false;
            }

            return Objects.equals(pa.getOwnerType(), pb.getOwnerType())
                   && pa.getRawType().equals(pb.getRawType())
                   && Arrays.equals(pa.getActualTypeArguments(), pb.getActualTypeArguments());
        } else if(a instanceof GenericArrayType ga) {
            if(!(b instanceof GenericArrayType gb)) {
                return false;
            }

            return equals(ga.getGenericComponentType(), gb.getGenericComponentType());
        } else if(a instanceof WildcardType wa) {
            if(!(b instanceof WildcardType wb)) {
                return false;
            }

            return Arrays.equals(wa.getUpperBounds(), wb.getUpperBounds())
                   && Arrays.equals(wa.getLowerBounds(), wb.getLowerBounds());
        } else if(a instanceof TypeVariable<?> va) {
            if(!(b instanceof TypeVariable<?> vb)) {
                return false;
            }
            return va.getGenericDeclaration() == vb.getGenericDeclaration()
                   && va.getName().equals(vb.getName());
        } else {
            return false;
        }
    }

    private record ClassField(Class clss, String fild) {};
}
