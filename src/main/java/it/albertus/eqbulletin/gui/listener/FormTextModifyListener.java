package it.albertus.eqbulletin.gui.listener;

import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;

import it.albertus.eqbulletin.gui.SearchForm;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FormTextModifyListener implements ModifyListener {

	@NonNull
	private final SearchForm form;

	@Override
	public void modifyText(final ModifyEvent e) {
		form.getSearchButton().setEnabled(form.isValid());
	}

}
