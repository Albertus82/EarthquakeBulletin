package it.albertus.eqbulletin.gui;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import it.albertus.eqbulletin.config.EarthquakeBulletinConfig;
import it.albertus.eqbulletin.gui.job.DownloadMapJob;
import it.albertus.eqbulletin.model.MapImage;
import it.albertus.eqbulletin.resources.Messages;
import it.albertus.eqbulletin.service.MapCache;
import it.albertus.jface.EnhancedErrorDialog;
import it.albertus.jface.HqImageResizer;
import it.albertus.util.logging.LoggerFactory;

public class MapCanvas {

	public static class Defaults {
		public static final boolean MAP_RESIZE_HQ = true;

		private Defaults() {
			throw new IllegalAccessError("Constants class");
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(MapCanvas.class);

	private final MapCache cache = new MapCache();
	private final Canvas canvas;

	private String guid;
	private Image image;

	private Image resized;

	private DownloadMapJob downloadMapJob;

	private final MenuItem downloadMenuItem;

	public MapCanvas(final Composite parent) {
		canvas = new Canvas(parent, SWT.BORDER);
		canvas.setBackground(getBackgroundColor());
		canvas.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(final PaintEvent e) {
				paintImage();
			}
		});

		final Menu contextMenu = new Menu(canvas);
		downloadMenuItem = new MenuItem(contextMenu, SWT.PUSH);
		downloadMenuItem.setText(Messages.get("lbl.menu.item.save.map"));
		downloadMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				saveImage();
			}
		});
		canvas.setMenu(contextMenu);
		canvas.addMenuDetectListener(new MenuDetectListener() {
			@Override
			public void menuDetected(final MenuDetectEvent e) {
				downloadMenuItem.setEnabled(canSaveImage());
			}
		});
	}

	public Image getImage() {
		return image;
	}

	public String getGuid() {
		return guid;
	}

	public void setImage(final String guid, final MapImage mapImage) {
		final byte[] imageBytes = mapImage.getBytes();
		if (imageBytes != null && imageBytes.length > 0) {
			cache.put(guid, mapImage);
			try (final ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
				final Image oldImage = this.image;
				this.image = new Image(canvas.getDisplay(), bais);
				this.guid = guid;
				paintImage();
				if (oldImage != null) {
					oldImage.dispose();
				}
			}
			catch (final Exception e) {
				logger.log(Level.WARNING, e.toString(), e);
			}
		}
	}

	public void clear() {
		final GC gc = new GC(canvas);
		gc.setBackground(getBackgroundColor());
		final Rectangle canvasBounds = canvas.getBounds();
		gc.fillRectangle(0, 0, canvasBounds.width, canvasBounds.height);
		gc.dispose();
		if (image != null) {
			image.dispose();
			image = null;
		}
		guid = null;
	}

	private void paintImage() {
		if (image != null) {
			final Rectangle originalRect = image.getBounds();
			final Rectangle resizedRect = getResizedRectangle();

			final GC gc = new GC(canvas);

			if (resizedRect.height == originalRect.height) { // Do not resize!
				gc.drawImage(image, resizedRect.x, resizedRect.y);
			}
			else {
				if (EarthquakeBulletinConfig.getInstance().getBoolean("map.resize.hq", Defaults.MAP_RESIZE_HQ)) {
					final Image oldImage = resized;
					final float scale = resizedRect.height / (float) originalRect.height;
					resized = HqImageResizer.resize(image, scale);
					gc.drawImage(resized, resizedRect.x, resizedRect.y);
					if (oldImage != null && oldImage != resized) {
						oldImage.dispose();
					}
				}
				else { // Fast low-quality resizing
					gc.drawImage(image, 0, 0, originalRect.width, originalRect.height, resizedRect.x, resizedRect.y, resizedRect.width, resizedRect.height);
				}
			}
			gc.dispose();
		}
	}

	private Rectangle getResizedRectangle() {
		final Rectangle imageSize = image.getBounds();
		final double imageRatio = 1.0 * imageSize.width / imageSize.height;
		final Rectangle canvasSize = canvas.getBounds();
		final double canvasRatio = 1.0 * canvasSize.width / canvasSize.height;

		int height;
		int width;

		if (canvasRatio > imageRatio) {
			width = (int) (imageSize.width * (1.0 * canvasSize.height / imageSize.height));
			height = canvasSize.height;
		}
		else {
			width = canvasSize.width;
			height = (int) (imageSize.height * (1.0 * canvasSize.width / imageSize.width));
		}

		// Allow reduction only
		if (width > imageSize.width || height > imageSize.height) {
			width = imageSize.width;
			height = imageSize.height;
		}

		final int y = (int) ((canvasSize.height - height) / 2.0);
		final int x = (int) ((canvasSize.width - width) / 2.0);

		return new Rectangle(x, y, width, height);
	}

	private boolean canSaveImage() {
		return image != null && guid != null;
	}

	private void saveImage() {
		if (canSaveImage()) {
			final FileDialog saveDialog = new FileDialog(canvas.getShell(), SWT.SAVE);
			saveDialog.setFilterExtensions(new String[] { "*.JPG;*.jpg" });
			saveDialog.setFileName(guid.toLowerCase() + ".jpg");
			saveDialog.setOverwrite(true);
			final String fileName = saveDialog.open();
			if (fileName != null && fileName.trim().isEmpty()) {
				try {
					Files.write(Paths.get(fileName), cache.get(guid).getBytes());
				}
				catch (final Exception e) {
					final String message = Messages.get("err.image.save", fileName);
					logger.log(Level.WARNING, message, e);
					EnhancedErrorDialog.openError(canvas.getShell(), Messages.get("lbl.window.title"), message, IStatus.WARNING, e, Images.getMainIcons());
				}
			}
		}
	}

	public void updateTexts() {
		downloadMenuItem.setText(Messages.get("lbl.menu.item.save.map"));
	}

	private Color getBackgroundColor() {
		return canvas.getDisplay().getSystemColor(SWT.COLOR_WHITE);
	}

	public Canvas getCanvas() {
		return canvas;
	}

	public MapCache getCache() {
		return cache;
	}

	public DownloadMapJob getDownloadMapJob() {
		return downloadMapJob;
	}

	public void setDownloadMapJob(DownloadMapJob downloadMapJob) {
		this.downloadMapJob = downloadMapJob;
	}

}