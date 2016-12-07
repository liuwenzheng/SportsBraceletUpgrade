package coach.oriental.com.sportsbraceletupgrade;

import android.text.TextUtils;

import com.elvishew.xlog.XLog;

/**
 * Log组件
 *
 * @author wenzheng.liu
 *
 */
public class LogModule {
	// tag
	public static String TAG = "sportsbraceletInit";

	public static boolean debug = true;

	// v
	public static void v(String msg) {
		v(null, msg, null);
	}

	public static void v(String tag, String msg) {
		v(tag, msg, null);
	}

	public static void v(String tag, String msg, Throwable thr) {
		if (!debug) {
			return;
		}
		if (TextUtils.isEmpty(tag)) {
			tag = TAG;
			XLog.v(tag, msg, thr);
		} else {
			XLog.v(TAG, msg, thr);
			XLog.v(tag, msg, thr);
		}
	}

	// i
	public static void i(String msg) {
		i(null, msg, null);
	}

	public static void i(String tag, String msg) {
		i(tag, msg, null);
	}

	public static void i(String tag, String msg, Throwable thr) {
		if (!debug) {
			return;
		}
		if (TextUtils.isEmpty(tag)) {
			tag = TAG;
			XLog.i(msg);
		} else {
			XLog.i(TAG, msg, thr);
			XLog.i(tag, msg, thr);
		}

	}

	// d
	public static void d(String msg) {
		d(null, msg, null);
	}

	public static void d(String tag, String msg) {
		d(tag, msg, null);
	}

	public static void d(String tag, String msg, Throwable thr) {
		if (!debug) {
			return;
		}
		if (TextUtils.isEmpty(tag)) {
			tag = TAG;
			XLog.d(msg);
		} else {
			XLog.d(TAG, msg, thr);
			XLog.d(tag, msg, thr);
		}

	}

	// w
	public static void w(String msg) {
		w(null, msg, null);
	}

	public static void w(String tag, String msg) {
		w(tag, msg, null);
	}

	public static void w(String tag, String msg, Throwable thr) {
		if (!debug) {
			return;
		}
		if (TextUtils.isEmpty(tag)) {
			tag = TAG;
			XLog.w(tag, msg, thr);
		} else {
			XLog.w(TAG, msg, thr);
			XLog.w(tag, msg, thr);
		}
	}

	// e
	public static void e(String msg) {
		e(null, msg, null);
	}

	public static void e(String tag, String msg) {
		e(tag, msg, null);
	}

	public static void e(String tag, String msg, Throwable thr) {
		if (!debug) {
			return;
		}
		if (TextUtils.isEmpty(tag)) {
			tag = TAG;
			XLog.e(tag, msg, thr);
		} else {
			XLog.e(TAG, msg, thr);
			XLog.e(tag, msg, thr);
		}
	}

}
