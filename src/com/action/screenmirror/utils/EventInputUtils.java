package com.action.screenmirror.utils;

import android.hardware.input.InputManager;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * /** Created by tianluhua on 21/7/17.
 * 
 * Class to create seamless input/touch events on your Android device without
 * root
 */
public class EventInputUtils {

	private static Method injectInputEventMethod;
	private static InputManager im;

	static {

		// Get the instance of InputManager class using reflection
		String methodName = "getInstance";
		Object[] objArr = new Object[0];
		try {
			im = (InputManager) InputManager.class.getDeclaredMethod(
					methodName, new Class[0]).invoke(null, objArr);
		} catch (IllegalAccessException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IllegalArgumentException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (InvocationTargetException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (NoSuchMethodException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// Make MotionEvent.obtain() method accessible
		methodName = "obtain";
		try {
			MotionEvent.class.getDeclaredMethod(methodName, new Class[0])
					.setAccessible(true);
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Get the reference to injectInputEvent method
		methodName = "injectInputEvent";
		try {
			injectInputEventMethod = InputManager.class.getMethod(methodName,
					new Class[] { InputEvent.class, Integer.TYPE });
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public EventInputUtils() throws Exception {

	}

	public static void injectMotionEvent(int inputSource, int action,
			long when, float x, float y, float pressure)
			throws InvocationTargetException, IllegalAccessException {
		MotionEvent event = MotionEvent.obtain(when, when, action, x, y,
				pressure, 1.0f, 0, 1.0f, 1.0f, 0, 0);
		event.setSource(inputSource);
		injectInputEventMethod.invoke(im,
				new Object[] { event, Integer.valueOf(0) });
	}

	private static void injectKeyEvent(KeyEvent event)
			throws InvocationTargetException, IllegalAccessException {
		injectInputEventMethod.invoke(im,
				new Object[] { event, Integer.valueOf(0) });
	}
}
