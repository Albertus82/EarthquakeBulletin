package it.albertus.eqbulletin.gui.listener;

import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.widgets.Table;

import it.albertus.eqbulletin.gui.ResultsTable;
import it.albertus.eqbulletin.gui.ResultsTable.ContextMenu;
import it.albertus.eqbulletin.model.Earthquake;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ResultsTableContextMenuDetectListener implements MenuDetectListener {

	@NonNull
	private final ResultsTable resultsTable;

	@Override
	public void menuDetected(final MenuDetectEvent e) {
		final Earthquake selection = (Earthquake) resultsTable.getTableViewer().getStructuredSelection().getFirstElement();
		final ContextMenu contextMenu = resultsTable.getContextMenu();
		contextMenu.getShowMapMenuItem().setEnabled(selection != null && selection.getEnclosureUri().isPresent());
		contextMenu.getShowMomentTensorMenuItem().setEnabled(selection != null && selection.getMomentTensorUri().isPresent());
		contextMenu.getCopyLinkMenuItem().setEnabled(selection != null && selection.getLink().isPresent());
		contextMenu.getOpenBrowserMenuItem().setEnabled(selection != null && selection.getLink().isPresent());
		contextMenu.getGoogleMapsBrowserMenuItem().setEnabled(selection != null);
		contextMenu.getEpicenterMapPopupMenuItem().setEnabled(selection != null);
		final Table table = resultsTable.getTableViewer().getTable();
		contextMenu.getExportCsvMenuItem().setEnabled(table != null && table.getItemCount() > 0);
		contextMenu.getMenu().setVisible(true);
	}

}
