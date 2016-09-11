package it.albertus.geofon.client.gui.listener;

import it.albertus.geofon.client.gui.MapCanvas;

import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;

public class MapCanvasContextMenuListener implements MenuDetectListener {

	private final MapCanvas mapCanvas;

	public MapCanvasContextMenuListener(final MapCanvas mapCanvas) {
		this.mapCanvas = mapCanvas;
	}

	@Override
	public void menuDetected(final MenuDetectEvent mde) {
		mapCanvas.getDownloadMenuItem().setEnabled(mapCanvas.getImage() != null && mapCanvas.getGuid() != null);
	}

}
