package it.albertus.eqbulletin.gui.async;

import static it.albertus.jface.DisplayThreadExecutor.Mode.ASYNC;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import it.albertus.eqbulletin.gui.EarthquakeBulletinGui;
import it.albertus.eqbulletin.gui.Images;
import it.albertus.jface.DisplayThreadExecutor;
import it.albertus.jface.EnhancedErrorDialog;
import it.albertus.util.DaemonThreadFactory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;

@Log
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AsyncOperation {

	protected static final ThreadFactory threadFactory = new DaemonThreadFactory() {
		@Override
		public Thread newThread(final Runnable r) {
			final Thread thread = super.newThread(r);
			thread.setPriority(Thread.MIN_PRIORITY);
			thread.setUncaughtExceptionHandler((t, e) -> log.log(Level.SEVERE, t.toString(), e));
			return thread;
		}
	};

	private static final AtomicInteger operationCount = new AtomicInteger();

	static {
		System.setProperty(IJobManager.PROP_USE_DAEMON_THREADS, Boolean.TRUE.toString());
	}

	protected static void setAppStartingCursor(final Shell shell) {
		log.log(Level.FINE, "setAppStartingCursor() - operationCount = {0}", operationCount);
		if (operationCount.getAndIncrement() == 0 && shell != null && !shell.isDisposed()) {
			shell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_APPSTARTING));
		}
	}

	protected static void setDefaultCursor(final Shell shell) {
		if (operationCount.updateAndGet(o -> o > 1 ? o - 1 : 0) == 0 && shell != null && !shell.isDisposed()) {
			shell.setCursor(null);
		}
		log.log(Level.FINE, "setDefaultCursor() - operationCount = {0}", operationCount);
	}

	protected static void showErrorDialog(final AsyncOperationException e, final Shell shell) {
		log.log(e.getLoggingLevel(), e.getMessage(), e);
		if (!shell.isDisposed()) {
			new DisplayThreadExecutor(shell, ASYNC).execute(() -> EnhancedErrorDialog.openError(shell, EarthquakeBulletinGui.getApplicationName(), e.getMessage(), e.getSeverity(), e.getCause() != null ? e.getCause() : e, Images.getAppIconArray()));
		}
	}

}
