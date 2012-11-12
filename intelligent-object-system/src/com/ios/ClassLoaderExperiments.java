package com.ios;

import java.net.URL;
import java.net.URLClassLoader;

public class ClassLoaderExperiments {

	public static void main(String... args) {
		System.out.println(Thread.currentThread().getContextClassLoader());
		System.out.println(ClassLoaderExperiments.class.getClassLoader());
		ClassLoader loader = new URLClassLoader(new URL[]{}, Thread.currentThread().getContextClassLoader());
		
		Thread.currentThread().setContextClassLoader(loader);
		System.out.println(Thread.currentThread().getContextClassLoader());
		ClassLoaderExperiments obj = new ClassLoaderExperiments();
		System.out.println(obj.getClass().getClassLoader());
	}
	
}
