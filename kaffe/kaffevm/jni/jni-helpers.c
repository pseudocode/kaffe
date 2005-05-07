/*
 * jni-helpers.c
 * Java Native Interface - JNI helper functions (JNI 1.2).
 *
 * Copyright (c) 1996, 1997
 *	Transvirtual Technologies, Inc.  All rights reserved.
 *
 * Copyright (c) 2004
 *      The Kaffe.org's developers. See ChangeLog for details.
 *
 * See the file "license.terms" for information on usage and redistribution 
 * of this file. 
 */

#include "config.h"
#include "native.h"
#include "jni.h"
#include "jni_i.h"
#include "jni_funcs.h"
#include "native.h"
#include "classMethod.h"
#include "reflect.h"
#include "java_lang_reflect_Method.h"
#include "java_lang_reflect_Field.h"

jobject KaffeJNI_NewDirectByteBuffer(JNIEnv *env, void *buffer, jlong size)
{
  jclass clazz;
  jclass clazz_rawdata;
  jmethodID constructor;
  jmethodID constructor_rawdata;
  jobject bbuf;
  jobject rawdata;

  BEGIN_EXCEPTION_HANDLING(NULL);

  clazz = (*env)->FindClass(env, "java/nio/DirectByteBufferImpl$ReadWrite");
  constructor = (*env)->GetMethodID(env, clazz, "<init>", "(Ljava/lang/Object;Lgnu/classpath/RawData;III)V");
#if SIZEOF_VOID_P == 4
  clazz_rawdata = (*env)->FindClass(env, "gnu/classpath/RawData32");
  constructor_rawdata = (*env)->GetMethodID(env, clazz_rawdata, "<init>", "(I)V");
#elif SIZEOF_VOID_P == 8
  clazz_rawdata = (*env)->FindClass(env, "gnu/classpath/RawData64");
  constructor_rawdata = (*env)->GetMethodID(env, clazz_rawdata, "<init>", "(L)V");
#else
#error "Unknown void pointer width"
#endif

  rawdata = (*env)->NewObject(env, clazz_rawdata, constructor_rawdata, buffer);
  
  bbuf = (*env)->NewObject(env, clazz, constructor, NULL, rawdata, (jint)size, (jint)size, (jint)0);

  END_EXCEPTION_HANDLING();

  return bbuf;
}

void *KaffeJNI_GetDirectBufferAddress(JNIEnv *env, jobject buffer)
{
  jfieldID address_field;
  void *address;
  jclass clazz;

  BEGIN_EXCEPTION_HANDLING(NULL);

  clazz = (*env)->FindClass(env, "java/nio/DirectByteBufferImpl");

  if (!(*env)->IsInstanceOf(env, buffer, clazz))
    address = NULL;
  else
    {
      clazz = (*env)->GetObjectClass(env, buffer);
      address_field = (*env)->GetFieldID(env, clazz, "address", "Lgnu/classpath/RawData;");
      
      address = (void *)((*env)->GetObjectField(env, buffer, address_field));
    }

  END_EXCEPTION_HANDLING();

  return address;
}

long KaffeJNI_GetDirectBufferCapacity(JNIEnv *env UNUSED, jobject buffer)
{  
  jmethodID capacity_method;
  jint capacity;
  jclass clazz;

  BEGIN_EXCEPTION_HANDLING(-1);

  clazz = (*env)->FindClass(env, "java/nio/DirectByteBufferImpl");
  if (!(*env)->IsInstanceOf(env, buffer, clazz))
    capacity = -1;
  else
    {
      clazz = (*env)->GetObjectClass(env, buffer);
      capacity_method = (*env)->GetMethodID(env, clazz, "capacity", "()I");
      
      capacity = (*env)->CallIntMethod(env, buffer, capacity_method);
    }

  END_EXCEPTION_HANDLING();

  return capacity;
}


jmethodID
KaffeJNI_FromReflectedMethod (JNIEnv *env UNUSED, jobject method)
{
	Hjava_lang_reflect_Method *realMethod = (Hjava_lang_reflect_Method *)method;
	jmethodID id;

	BEGIN_EXCEPTION_HANDLING(NULL);
	
	id = (jmethodID) &(unhand(realMethod)->clazz->methods[unhand(realMethod)->slot]);

	END_EXCEPTION_HANDLING();

	return id;
}

jfieldID
KaffeJNI_FromReflectedField (JNIEnv *env UNUSED, jobject field)
{
	Hjava_lang_reflect_Field *realField = (Hjava_lang_reflect_Field *)field;
	jfieldID id;

	BEGIN_EXCEPTION_HANDLING(NULL);

	id = (jfieldID) &(unhand(realField)->clazz->fields[unhand(realField)->slot]);
	END_EXCEPTION_HANDLING();

	return id;
}

jobject
KaffeJNI_ToReflectedMethod (JNIEnv *env UNUSED, jclass cls, jmethodID mid, jboolean isStatic UNUSED)
{
        Hjava_lang_reflect_Method *refMeth;
	Hjava_lang_Class *clazz;
	Method *allMethods;
	int i;

	BEGIN_EXCEPTION_HANDLING(NULL);
	
	clazz = (Hjava_lang_Class *)cls;
	refMeth = NULL;
	for (allMethods = CLASS_METHODS(clazz), i = 0;
	     i < CLASS_NMETHODS(clazz); 
	     i++, allMethods++)
	  {
	    if (allMethods == (Method *)mid)
	      {
		refMeth = KaffeVM_makeReflectMethod(clazz, i);
		break;
	      }
	  }
	END_EXCEPTION_HANDLING();
	
	return (jobject) refMeth;
}

jobject
KaffeJNI_ToReflectedField (JNIEnv *env UNUSED, jclass cls, jfieldID fid, jboolean isStatic UNUSED)
{
        Hjava_lang_reflect_Field *refField;
	Hjava_lang_Class *clazz;
	Field *allFields;
	int i;

	BEGIN_EXCEPTION_HANDLING(NULL);

	clazz = (Hjava_lang_Class *)cls;
	refField = NULL;
	for (allFields = CLASS_FIELDS(clazz), i = 0;
	     i < CLASS_NFIELDS(clazz);
	     i++, allFields++)
	  {
	    if (allFields == (Field *)fid)
	      {
		refField = KaffeVM_makeReflectField(clazz, i);
		break;
	      }
	  }

	END_EXCEPTION_HANDLING();
	
	return (jobject) refField;
}

