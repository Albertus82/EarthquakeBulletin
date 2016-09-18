package it.albertus.earthquake.gui.map;

import it.albertus.earthquake.resources.Messages;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URL;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

public class MapDialog extends Dialog {

	public static final int DEFAULT_ZOOM = 1;

	protected static final String HTML_FILE_NAME = "map.html";
	protected static final int BUTTON_WIDTH = 90;

	private double centerLat;
	private double centerLng;
	private int zoom = DEFAULT_ZOOM;

	private Double northEastLat;
	private Double southWestLat;
	private Double northEastLng;
	private Double southWestLng;

	private volatile int returnCode = SWT.CANCEL;

	private Image[] images;

	public MapDialog(final Shell parent) {
		this(parent, SWT.SHEET | SWT.RESIZE | SWT.MAX);
	}

	public MapDialog(final Shell parent, final int style) {
		super(parent, style);
	}

	public int open() {
		final Shell shell = new Shell(getParent(), getStyle());
		shell.setText(getText());
		final Image[] images = getImages();
		if (images != null && images.length > 0) {
			shell.setImages(images);
		}
		createContents(shell);
		final Point normalShellSize = shell.getSize();
		final Point packedShellSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		shell.setMinimumSize(packedShellSize);
		shell.setSize(Math.min(packedShellSize.x * 3, normalShellSize.x), Math.min(packedShellSize.y * 3, normalShellSize.y));
		shell.open();
		final Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return returnCode;
	}

	protected void createContents(final Shell shell) {
		GridLayoutFactory.swtDefaults().applyTo(shell);

		final Browser browser = new Browser(shell, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(browser);
		browser.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent ce) {
				try {
					final Point browserSize = browser.getSize();
					browser.execute("document.getElementById('map_canvas').style.width= " + (browserSize.x - 20) + ";");
					browser.execute("document.getElementById('map_canvas').style.height= " + (browserSize.y - 20) + ";");
				}
				catch (final Exception e) {
					e.printStackTrace();
				}
			}
		});

		final URL pageUrl = getMapPage(shell);
		browser.setUrl(pageUrl != null ? pageUrl.toString() : "");

		final Composite buttonComposite = new Composite(shell, SWT.NONE);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(buttonComposite);
		GridDataFactory.swtDefaults().align(SWT.CENTER, SWT.CENTER).grab(true, false).applyTo(buttonComposite);

		final Button confirmButton = new Button(buttonComposite, SWT.PUSH);
		confirmButton.setText(Messages.get("lbl.button.confirm"));
		GridDataFactory.swtDefaults().align(SWT.CENTER, SWT.FILL).grab(true, false).minSize(BUTTON_WIDTH, SWT.DEFAULT).applyTo(confirmButton);
		confirmButton.setFocus();
		confirmButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent se) {
				try {
					centerLat = (double) browser.evaluate("return map.getCenter().lat();");
					centerLng = (double) browser.evaluate("return map.getCenter().lng();");
					zoom = ((Number) browser.evaluate("return map.getZoom();")).intValue();
					northEastLat = (Double) browser.evaluate("return map.getBounds().getNorthEast().lat();");
					southWestLat = (Double) browser.evaluate("return map.getBounds().getSouthWest().lat();");
					northEastLng = (Double) browser.evaluate("return map.getBounds().getNorthEast().lng();");
					southWestLng = (Double) browser.evaluate("return map.getBounds().getSouthWest().lng();");
					returnCode = SWT.OK;
				}
				catch (final SWTException swte) {/* Ignore */}
				catch (final Exception e) {
					e.printStackTrace();
				}
				finally {
					shell.close();
				}
			}
		});

		final Button cancelButton = new Button(buttonComposite, SWT.PUSH);
		cancelButton.setText(Messages.get("lbl.button.cancel"));
		GridDataFactory.swtDefaults().align(SWT.CENTER, SWT.FILL).grab(true, false).minSize(BUTTON_WIDTH, SWT.DEFAULT).applyTo(cancelButton);
		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent se) {
				shell.close();
			}
		});
		shell.setDefaultButton(confirmButton);
	}

	protected URL getMapPage(final Shell shell) {
		URL pageUrl = null;
		File tempFile = null;
		BufferedReader reader = null;
		BufferedWriter writer = null;
		try {
			reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(HTML_FILE_NAME)));
			tempFile = File.createTempFile("map", null);
			writer = new BufferedWriter(new FileWriter(tempFile));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.contains("center : new google.maps.LatLng(0, 0)")) {
					line = line.replace("0, 0", centerLat + ", " + centerLng);
				}
				else if (line.contains("zoom : 1")) {
					line = line.replace("1", Integer.toString(zoom));
				}
				writer.write(line);
				writer.newLine();
			}
			pageUrl = tempFile.toURI().toURL();
		}
		catch (final Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				writer.close();
			}
			catch (final Exception e) {/* Ignore */}
			try {
				reader.close();
			}
			catch (final Exception e) {/* Ignore */}
		}

		if (tempFile != null) {
			final File fileToDelete = tempFile;
			shell.addListener(SWT.Close, new Listener() {
				@Override
				public void handleEvent(final Event event) {
					try {
						fileToDelete.delete();
					}
					catch (final Exception e) {
						e.printStackTrace();
					}
				}
			});
		}
		return pageUrl;
	}

	public double getCenterLat() {
		return centerLat;
	}

	public void setCenterLat(final double centerLat) {
		this.centerLat = centerLat;
	}

	public double getCenterLng() {
		return centerLng;
	}

	public void setCenterLng(final double centerLng) {
		this.centerLng = centerLng;
	}

	public int getZoom() {
		return zoom;
	}

	public void setZoom(final int zoom) {
		this.zoom = zoom;
	}

	public Double getNorthEastLat() {
		return northEastLat;
	}

	public void setNorthEastLat(final Double northEastLat) {
		this.northEastLat = northEastLat;
	}

	public Double getSouthWestLat() {
		return southWestLat;
	}

	public void setSouthWestLat(final Double southWestLat) {
		this.southWestLat = southWestLat;
	}

	public Double getNorthEastLng() {
		return northEastLng;
	}

	public void setNorthEastLng(final Double northEastLng) {
		this.northEastLng = northEastLng;
	}

	public Double getSouthWestLng() {
		return southWestLng;
	}

	public void setSouthWestLng(final Double southWestLng) {
		this.southWestLng = southWestLng;
	}

	public int getReturnCode() {
		return returnCode;
	}

	public void setReturnCode(final int returnCode) {
		this.returnCode = returnCode;
	}

	public Image[] getImages() {
		return images;
	}

	public void setImages(final Image[] images) {
		this.images = images;
	}

}
