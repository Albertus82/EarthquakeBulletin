package it.albertus.eqbulletin.gui;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.nebula.widgets.cdatetime.CDT;
import org.eclipse.nebula.widgets.cdatetime.CDateTime;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import it.albertus.eqbulletin.config.EarthquakeBulletinConfig;
import it.albertus.eqbulletin.gui.decoration.SearchFormControlValidatorDecoration;
import it.albertus.eqbulletin.gui.job.SearchJob;
import it.albertus.eqbulletin.gui.listener.AutoRefreshButtonSelectionListener;
import it.albertus.eqbulletin.gui.listener.ClearButtonSelectionListener;
import it.albertus.eqbulletin.gui.listener.FormFieldTraverseListener;
import it.albertus.eqbulletin.gui.listener.FormTextModifyListener;
import it.albertus.eqbulletin.gui.listener.FormatRadioSelectionListener;
import it.albertus.eqbulletin.gui.listener.MapButtonSelectionListener;
import it.albertus.eqbulletin.gui.listener.SearchButtonSelectionListener;
import it.albertus.eqbulletin.gui.listener.StopButtonSelectionListener;
import it.albertus.eqbulletin.model.Format;
import it.albertus.eqbulletin.resources.Messages;
import it.albertus.jface.JFaceMessages;
import it.albertus.jface.SwtUtils;
import it.albertus.jface.decoration.ControlValidatorDecoration;
import it.albertus.jface.google.maps.MapBoundsDialog;
import it.albertus.jface.google.maps.MapControl;
import it.albertus.jface.google.maps.MapOptions;
import it.albertus.jface.google.maps.MapType;
import it.albertus.jface.listener.FloatVerifyListener;
import it.albertus.jface.listener.IntegerVerifyListener;
import it.albertus.jface.validation.ControlValidator;
import it.albertus.jface.validation.FloatTextValidator;
import it.albertus.jface.validation.IntegerTextValidator;
import it.albertus.jface.validation.Validator;
import it.albertus.util.Configuration;
import it.albertus.util.Localized;
import it.albertus.util.logging.LoggerFactory;

public class SearchForm {

	private static final Logger logger = LoggerFactory.getLogger(SearchForm.class);

	public static final int COORDINATES_TEXT_LIMIT = 7;
	public static final int MAGNITUDE_TEXT_LIMIT = 4;
	public static final int PERIOD_TEXT_LIMIT = 10;
	public static final int RESULTS_TEXT_LIMIT = 4;
	public static final int AUTOREFRESH_TEXT_LIMIT = 4;

	public static final float LATITUDE_MIN_VALUE = -90;
	public static final float LATITUDE_MAX_VALUE = 90;
	public static final float LONGITUDE_MIN_VALUE = -180;
	public static final float LONGITUDE_MAX_VALUE = 180;
	public static final float MAGNITUDE_MIN_VALUE = 0;
	public static final float MAGNITUDE_MAX_VALUE = 10;
	public static final int RESULTS_MIN_VALUE = 1;
	public static final int RESULTS_MAX_VALUE = 1000;
	public static final int AUTOREFRESH_MIN_VALUE = 1;

	public static final String DATE_PATTERN = "yyyy-MM-dd";

	private static final byte CDATETIME_INDENT_RIGHT = 3;

	private static final String MSG_KEY_ERR_INTEGER_MIN = "err.preferences.integer.min";
	private static final String MSG_KEY_ERR_INTEGER_RANGE = "err.preferences.integer.range";
	private static final String MSG_KEY_ERR_DECIMAL_RANGE = "err.preferences.decimal.range";

	private static final ThreadLocal<DateFormat> dateFormat = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			final DateFormat df = new SimpleDateFormat(DATE_PATTERN);
			df.setLenient(false);
			return df;
		}
	};

	public static class Defaults {
		public static final boolean AUTOREFRESH_ENABLED = false;
		public static final boolean CRITERIA_RESTRICT = false;
		public static final Format FORMAT = Format.HTML;

		private Defaults() {
			throw new IllegalAccessError("Constants class");
		}
	}

	private static final Configuration configuration = EarthquakeBulletinConfig.getInstance();

	private final Composite formComposite;

	private final Label periodLabel;
	private final Label periodFromLabel;
	private final Label periodToLabel;
	private final CDateTime periodFromDateTime;
	private final CDateTime periodToDateTime;
	private final Label periodFromNote;
	private final Label periodToNote;

	private final Label latitudeLabel;
	private final Label latitudeFromLabel;
	private final Text latitudeFromText;
	private final Label latitudeFromNote;
	private final Label latitudeToLabel;
	private final Text latitudeToText;
	private final Label latitudeToNote;

	private final Label longitudeLabel;
	private final Label longitudeFromLabel;
	private final Text longitudeFromText;
	private final Label longitudeFromNote;
	private final Label longitudeToLabel;
	private final Text longitudeToText;
	private final Label longitudeToNote;

	private final Label minimumMagnitudeLabel;
	private final Text minimumMagnitudeText;
	private final Button restrictButton;

	private final Label outputFormatLabel;
	private final Composite radioComposite;
	private final Map<Format, Button> formatRadios = new EnumMap<>(Format.class);
	private final Label resultsLabel;
	private final Text resultsText;

	private final Group criteriaGroup;

	private final Composite buttonsComposite;
	private final Button searchButton;
	private final Label resultsNote;
	private final Button autoRefreshButton;
	private final Text autoRefreshText;
	private final Button stopButton;
	private final Button clearButton;
	private final Button openMap;

	private final MapBoundsDialog mapBoundsDialog;

	private final TraverseListener formFieldTraverseListener = new FormFieldTraverseListener(this);
	private final ModifyListener formTextModifyListener = new FormTextModifyListener(this);
	private final VerifyListener coordinatesVerifyListener = new FloatVerifyListener(true);

	private final Set<Validator> validators = new HashSet<>();

	private SearchJob searchJob;

	public SearchForm(final EarthquakeBulletinGui gui) {
		formComposite = new Composite(gui.getShell(), SWT.NONE);
		GridLayoutFactory.swtDefaults().margins(0, 0).numColumns(2).applyTo(formComposite);
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.TOP).grab(true, false).applyTo(formComposite);

		criteriaGroup = new Group(formComposite, SWT.NONE);
		criteriaGroup.setText(Messages.get("lbl.form.criteria.group"));
		GridLayoutFactory.swtDefaults().numColumns(7).applyTo(criteriaGroup);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(criteriaGroup);

		periodLabel = new Label(criteriaGroup, SWT.NONE);
		periodLabel.setText(Messages.get("lbl.form.criteria.period"));
		periodFromLabel = new Label(criteriaGroup, SWT.NONE);
		periodFromLabel.setText(Messages.get("lbl.form.criteria.period.from"));
		periodFromDateTime = new CDateTime(criteriaGroup, CDT.DROP_DOWN | CDT.BORDER);
		periodFromDateTime.setPattern(DATE_PATTERN);
		periodFromDateTime.setLocale(Messages.Language.ENGLISH.equals(Messages.getLanguage()) ? Locale.US : Messages.getLanguage().getLocale());
		periodFromDateTime.addTraverseListener(formFieldTraverseListener);
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).indent(CDATETIME_INDENT_RIGHT, 0).applyTo(periodFromDateTime);
		periodFromNote = new Label(criteriaGroup, SWT.NONE);
		periodFromNote.setText(Messages.get("lbl.form.criteria.period.from.note"));
		periodToLabel = new Label(criteriaGroup, SWT.NONE);
		periodToLabel.setText(Messages.get("lbl.form.criteria.period.to"));
		periodToDateTime = new CDateTime(criteriaGroup, CDT.DROP_DOWN | CDT.BORDER);
		periodToDateTime.setPattern(DATE_PATTERN);
		periodToDateTime.setLocale(Messages.Language.ENGLISH.equals(Messages.getLanguage()) ? Locale.US : Messages.getLanguage().getLocale());
		periodToDateTime.addTraverseListener(formFieldTraverseListener);
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).indent(CDATETIME_INDENT_RIGHT, 0).applyTo(periodToDateTime);
		periodToNote = new Label(criteriaGroup, SWT.NONE);
		periodToNote.setText(Messages.get("lbl.form.criteria.period.to.note"));

		latitudeLabel = new Label(criteriaGroup, SWT.NONE);
		latitudeLabel.setText(Messages.get("lbl.form.criteria.latitude"));
		latitudeFromLabel = new Label(criteriaGroup, SWT.NONE);
		latitudeFromLabel.setText(Messages.get("lbl.form.criteria.latitude.from"));
		latitudeFromText = new Text(criteriaGroup, SWT.BORDER);
		latitudeFromText.setTextLimit(COORDINATES_TEXT_LIMIT);
		latitudeFromText.addTraverseListener(formFieldTraverseListener);
		latitudeFromText.addVerifyListener(coordinatesVerifyListener);
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(latitudeFromText);
		latitudeFromNote = new Label(criteriaGroup, SWT.NONE);
		latitudeFromNote.setText(Messages.get("lbl.form.criteria.latitude.from.note"));
		latitudeToLabel = new Label(criteriaGroup, SWT.NONE);
		latitudeToLabel.setText(Messages.get("lbl.form.criteria.latitude.to"));
		latitudeToText = new Text(criteriaGroup, SWT.BORDER);
		latitudeToText.setTextLimit(COORDINATES_TEXT_LIMIT);
		latitudeToText.addTraverseListener(formFieldTraverseListener);
		latitudeToText.addVerifyListener(coordinatesVerifyListener);
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(latitudeToText);
		latitudeToNote = new Label(criteriaGroup, SWT.NONE);
		latitudeToNote.setText(Messages.get("lbl.form.criteria.latitude.to.note"));

		longitudeLabel = new Label(criteriaGroup, SWT.NONE);
		longitudeLabel.setText(Messages.get("lbl.form.criteria.longitude"));
		longitudeFromLabel = new Label(criteriaGroup, SWT.NONE);
		longitudeFromLabel.setText(Messages.get("lbl.form.criteria.longitude.from"));
		longitudeFromText = new Text(criteriaGroup, SWT.BORDER);
		longitudeFromText.setTextLimit(COORDINATES_TEXT_LIMIT);
		longitudeFromText.addTraverseListener(formFieldTraverseListener);
		longitudeFromText.addVerifyListener(coordinatesVerifyListener);
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(longitudeFromText);
		longitudeFromNote = new Label(criteriaGroup, SWT.NONE);
		longitudeFromNote.setText(Messages.get("lbl.form.criteria.longitude.from.note"));
		longitudeToLabel = new Label(criteriaGroup, SWT.NONE);
		longitudeToLabel.setText(Messages.get("lbl.form.criteria.longitude.to"));
		longitudeToText = new Text(criteriaGroup, SWT.BORDER);
		longitudeToText.setTextLimit(COORDINATES_TEXT_LIMIT);
		longitudeToText.addTraverseListener(formFieldTraverseListener);
		longitudeToText.addVerifyListener(coordinatesVerifyListener);
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(longitudeToText);
		longitudeToNote = new Label(criteriaGroup, SWT.NONE);
		longitudeToNote.setText(Messages.get("lbl.form.criteria.longitude.to.note"));

		minimumMagnitudeLabel = new Label(criteriaGroup, SWT.NONE);
		minimumMagnitudeLabel.setText(Messages.get("lbl.form.criteria.magnitude"));
		GridDataFactory.swtDefaults().span(2, 1).applyTo(minimumMagnitudeLabel);
		minimumMagnitudeText = new Text(criteriaGroup, SWT.BORDER);
		minimumMagnitudeText.setTextLimit(MAGNITUDE_TEXT_LIMIT);
		minimumMagnitudeText.addTraverseListener(formFieldTraverseListener);
		minimumMagnitudeText.addVerifyListener(new FloatVerifyListener(false));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(minimumMagnitudeText);
		restrictButton = new Button(criteriaGroup, SWT.CHECK);
		restrictButton.setText(Messages.get("lbl.form.criteria.restrict"));
		restrictButton.setSelection(configuration.getBoolean("criteria.restrict", Defaults.CRITERIA_RESTRICT));
		GridDataFactory.swtDefaults().span(4, 1).applyTo(restrictButton);

		outputFormatLabel = new Label(criteriaGroup, SWT.NONE);
		outputFormatLabel.setText(Messages.get("lbl.form.format"));
		radioComposite = new Composite(criteriaGroup, SWT.NONE);
		radioComposite.setLayout(new RowLayout(SWT.HORIZONTAL));
		GridDataFactory.swtDefaults().grab(false, false).span(2, 1).applyTo(radioComposite);
		Format selectedFormat;
		try {
			selectedFormat = Format.valueOf(configuration.getString("criteria.format", Defaults.FORMAT.name()).trim().toUpperCase());
		}
		catch (final IllegalArgumentException e) {
			logger.log(Level.WARNING, e.toString(), e);
			selectedFormat = Defaults.FORMAT;
		}
		for (final Format format : Format.values()) {
			final Button radio = new Button(radioComposite, SWT.RADIO);
			radio.addSelectionListener(new FormatRadioSelectionListener(this, radio, format));
			radio.setText(format.getLabel());
			radio.setSelection(format.equals(selectedFormat));
			formatRadios.put(format, radio);
		}
		resultsLabel = new Label(criteriaGroup, SWT.NONE);
		resultsLabel.setText(Messages.get("lbl.form.limit"));
		GridDataFactory.swtDefaults().grab(false, false).span(2, 1).applyTo(resultsLabel);
		resultsText = new Text(criteriaGroup, SWT.BORDER);
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(resultsText);
		resultsText.setTextLimit(RESULTS_TEXT_LIMIT);
		resultsText.addTraverseListener(formFieldTraverseListener);
		resultsText.addVerifyListener(new IntegerVerifyListener(false));
		resultsNote = new Label(criteriaGroup, SWT.NONE);
		resultsNote.setText(Messages.get("lbl.form.limit.note"));

		// Buttons
		buttonsComposite = new Composite(formComposite, SWT.NONE);
		GridLayoutFactory.swtDefaults().numColumns(2).equalWidth(true).applyTo(buttonsComposite);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(buttonsComposite);

		autoRefreshButton = new Button(buttonsComposite, SWT.CHECK);
		autoRefreshButton.setText(Messages.get("lbl.form.button.autorefresh"));
		autoRefreshButton.setSelection(configuration.getBoolean("autorefresh.enabled", Defaults.AUTOREFRESH_ENABLED));
		GridDataFactory.swtDefaults().span(2, 1).applyTo(autoRefreshButton);

		autoRefreshText = new Text(buttonsComposite, SWT.BORDER);
		autoRefreshText.setTextLimit(AUTOREFRESH_TEXT_LIMIT);
		autoRefreshText.addTraverseListener(formFieldTraverseListener);
		autoRefreshText.addVerifyListener(new IntegerVerifyListener(false));
		GridDataFactory.swtDefaults().span(2, 1).align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(autoRefreshText);

		searchButton = new Button(buttonsComposite, SWT.NONE);
		searchButton.setText(Messages.get("lbl.form.button.submit"));
		GridDataFactory.fillDefaults().grab(true, false).minSize(SwtUtils.convertHorizontalDLUsToPixels(searchButton, IDialogConstants.BUTTON_WIDTH), SWT.DEFAULT).applyTo(searchButton);

		stopButton = new Button(buttonsComposite, SWT.NONE);
		stopButton.setText(Messages.get("lbl.form.button.stop"));
		GridDataFactory.fillDefaults().grab(true, false).minSize(SwtUtils.convertHorizontalDLUsToPixels(stopButton, IDialogConstants.BUTTON_WIDTH), SWT.DEFAULT).applyTo(stopButton);
		stopButton.setEnabled(false);

		openMap = new Button(buttonsComposite, SWT.NONE);
		openMap.setText(Messages.get("lbl.form.button.map"));
		GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(openMap);
		openMap.addSelectionListener(new MapButtonSelectionListener(this));

		clearButton = new Button(buttonsComposite, SWT.NONE);
		clearButton.setText(Messages.get("lbl.form.button.clear"));
		GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(clearButton);

		// Listeners
		searchButton.addSelectionListener(new SearchButtonSelectionListener(gui));
		stopButton.addSelectionListener(new StopButtonSelectionListener(this));
		clearButton.addSelectionListener(new ClearButtonSelectionListener(this));
		autoRefreshButton.addSelectionListener(new AutoRefreshButtonSelectionListener(this));
		autoRefreshButton.notifyListeners(SWT.Selection, null);

		// Decorators
		ControlValidator<Text> validator = new FloatTextValidator(latitudeFromText, true, LATITUDE_MIN_VALUE, LATITUDE_MAX_VALUE);
		new SearchFormControlValidatorDecoration(validator, new Localized() {
			@Override
			public String getString() {
				return JFaceMessages.get(MSG_KEY_ERR_DECIMAL_RANGE, LATITUDE_MIN_VALUE, LATITUDE_MAX_VALUE);
			}
		});
		validators.add(validator);

		validator = new FloatTextValidator(latitudeToText, true, LATITUDE_MIN_VALUE, LATITUDE_MAX_VALUE);
		new SearchFormControlValidatorDecoration(validator, new Localized() {
			@Override
			public String getString() {
				return JFaceMessages.get(MSG_KEY_ERR_DECIMAL_RANGE, LATITUDE_MIN_VALUE, LATITUDE_MAX_VALUE);
			}
		});
		validators.add(validator);

		validator = new FloatTextValidator(longitudeFromText, true, LONGITUDE_MIN_VALUE, LONGITUDE_MAX_VALUE);
		new SearchFormControlValidatorDecoration(validator, new Localized() {
			@Override
			public String getString() {
				return JFaceMessages.get(MSG_KEY_ERR_DECIMAL_RANGE, LONGITUDE_MIN_VALUE, LONGITUDE_MAX_VALUE);
			}
		});
		validators.add(validator);

		validator = new FloatTextValidator(longitudeToText, true, LONGITUDE_MIN_VALUE, LONGITUDE_MAX_VALUE);
		new SearchFormControlValidatorDecoration(validator, new Localized() {
			@Override
			public String getString() {
				return JFaceMessages.get(MSG_KEY_ERR_DECIMAL_RANGE, LONGITUDE_MIN_VALUE, LONGITUDE_MAX_VALUE);
			}
		});
		validators.add(validator);

		validator = new FloatTextValidator(minimumMagnitudeText, true, MAGNITUDE_MIN_VALUE, MAGNITUDE_MAX_VALUE);
		new SearchFormControlValidatorDecoration(validator, new Localized() {
			@Override
			public String getString() {
				return JFaceMessages.get(MSG_KEY_ERR_DECIMAL_RANGE, MAGNITUDE_MIN_VALUE, MAGNITUDE_MAX_VALUE);
			}
		});
		validators.add(validator);

		validator = new IntegerTextValidator(resultsText, true, RESULTS_MIN_VALUE, RESULTS_MAX_VALUE);
		new SearchFormControlValidatorDecoration(validator, new Localized() {
			@Override
			public String getString() {
				return JFaceMessages.get(MSG_KEY_ERR_INTEGER_RANGE, RESULTS_MIN_VALUE, RESULTS_MAX_VALUE);
			}
		});
		validators.add(validator);

		validator = new IntegerTextValidator(autoRefreshText, true, AUTOREFRESH_MIN_VALUE, null);
		new ControlValidatorDecoration(validator, new Localized() {
			@Override
			public String getString() {
				return JFaceMessages.get(MSG_KEY_ERR_INTEGER_MIN, AUTOREFRESH_MIN_VALUE);
			}
		});
		validators.add(validator);

		// Text modify listeners
		latitudeFromText.addModifyListener(formTextModifyListener);
		latitudeToText.addModifyListener(formTextModifyListener);
		longitudeFromText.addModifyListener(formTextModifyListener);
		longitudeToText.addModifyListener(formTextModifyListener);
		minimumMagnitudeText.addModifyListener(formTextModifyListener);
		resultsText.addModifyListener(formTextModifyListener);
		autoRefreshText.addModifyListener(formTextModifyListener);

		// Load parameters from configuration
		periodFromDateTime.setSelection(getConfiguredDate("criteria.period.from"));
		periodToDateTime.setSelection(getConfiguredDate("criteria.period.to"));
		latitudeFromText.setText(getConfiguredFloatString("criteria.latitude.from"));
		latitudeToText.setText(getConfiguredFloatString("criteria.latitude.to"));
		longitudeFromText.setText(getConfiguredFloatString("criteria.longitude.from"));
		longitudeToText.setText(getConfiguredFloatString("criteria.longitude.to"));
		minimumMagnitudeText.setText(getConfiguredFloatString("criteria.magnitude"));
		resultsText.setText(getConfiguredIntegerString("criteria.limit"));
		autoRefreshText.setText(getConfiguredIntegerString("autorefresh.mins"));

		// Map
		mapBoundsDialog = new MapBoundsDialog(gui.getShell());
		mapBoundsDialog.setText(Messages.get("lbl.map.bounds.title"));
		mapBoundsDialog.setImages(Images.getMainIcons());
		final MapOptions mapOptions = mapBoundsDialog.getOptions();
		mapOptions.setType(MapType.TERRAIN);
		mapOptions.getControls().put(MapControl.SCALE, true);
	}

	public boolean isValid() {
		for (final Validator validator : validators) {
			if (!validator.isValid()) {
				return false;
			}
		}
		return true;
	}

	public void updateButtons() {
		if (searchJob == null || searchJob.getState() == Job.NONE) {
			stopButton.setEnabled(false);
			searchButton.setEnabled(isValid());
		}
		else {
			searchButton.setEnabled(false);
			stopButton.setEnabled(true);
		}
	}

	public void updateTexts() {
		periodFromDateTime.setLocale(Messages.Language.ENGLISH.equals(Messages.getLanguage()) ? Locale.US : Messages.getLanguage().getLocale());
		periodToDateTime.setLocale(Messages.Language.ENGLISH.equals(Messages.getLanguage()) ? Locale.US : Messages.getLanguage().getLocale());
		criteriaGroup.setText(Messages.get("lbl.form.criteria.group"));
		periodLabel.setText(Messages.get("lbl.form.criteria.period"));
		periodFromLabel.setText(Messages.get("lbl.form.criteria.period.from"));
		periodFromNote.setText(Messages.get("lbl.form.criteria.period.from.note"));
		periodToLabel.setText(Messages.get("lbl.form.criteria.period.to"));
		periodToNote.setText(Messages.get("lbl.form.criteria.period.to.note"));
		latitudeLabel.setText(Messages.get("lbl.form.criteria.latitude"));
		latitudeFromLabel.setText(Messages.get("lbl.form.criteria.latitude.from"));
		latitudeFromNote.setText(Messages.get("lbl.form.criteria.latitude.from.note"));
		latitudeToLabel.setText(Messages.get("lbl.form.criteria.latitude.to"));
		latitudeToNote.setText(Messages.get("lbl.form.criteria.latitude.to.note"));
		longitudeLabel.setText(Messages.get("lbl.form.criteria.longitude"));
		longitudeFromLabel.setText(Messages.get("lbl.form.criteria.longitude.from"));
		longitudeFromNote.setText(Messages.get("lbl.form.criteria.longitude.from.note"));
		longitudeToLabel.setText(Messages.get("lbl.form.criteria.longitude.to"));
		longitudeToNote.setText(Messages.get("lbl.form.criteria.longitude.to.note"));
		minimumMagnitudeLabel.setText(Messages.get("lbl.form.criteria.magnitude"));
		restrictButton.setText(Messages.get("lbl.form.criteria.restrict"));
		outputFormatLabel.setText(Messages.get("lbl.form.format"));
		resultsLabel.setText(Messages.get("lbl.form.limit"));
		resultsNote.setText(Messages.get("lbl.form.limit.note"));
		autoRefreshButton.setText(Messages.get("lbl.form.button.autorefresh"));
		searchButton.setText(Messages.get("lbl.form.button.submit"));
		stopButton.setText(Messages.get("lbl.form.button.stop"));
		clearButton.setText(Messages.get("lbl.form.button.clear"));
		openMap.setText(Messages.get("lbl.form.button.map"));
		mapBoundsDialog.setText(Messages.get("lbl.map.bounds.title"));
	}

	private String getConfiguredFloatString(final String key) {
		String value = "";
		try {
			final Float number = configuration.getFloat(key);
			if (number != null) {
				value = number.toString();
			}
		}
		catch (final RuntimeException e) {
			logger.log(Level.WARNING, e.toString(), e);
		}
		return value;
	}

	private String getConfiguredIntegerString(final String key) {
		String value = "";
		try {
			final Integer number = configuration.getInt(key);
			if (number != null) {
				value = number.toString();
			}
		}
		catch (final RuntimeException e) {
			logger.log(Level.WARNING, e.toString(), e);
		}
		return value;
	}

	private Date getConfiguredDate(final String key) {
		Date value = null;
		final String dateStr = configuration.getString(key);
		if (dateStr != null && !dateStr.trim().isEmpty()) {
			try {
				final DateFormat df = dateFormat.get();
				value = df.parse(dateStr);
			}
			catch (final Exception e) {
				logger.log(Level.WARNING, e.toString(), e);
			}
		}
		return value;
	}

	public SearchJob getSearchJob() {
		return searchJob;
	}

	public void setSearchJob(final SearchJob searchJob) {
		this.searchJob = searchJob;
	}

	public Composite getFormComposite() {
		return formComposite;
	}

	public Label getPeriodLabel() {
		return periodLabel;
	}

	public Label getPeriodFromLabel() {
		return periodFromLabel;
	}

	public Label getPeriodToLabel() {
		return periodToLabel;
	}

	public CDateTime getPeriodFromDateTime() {
		return periodFromDateTime;
	}

	public CDateTime getPeriodToDateTime() {
		return periodToDateTime;
	}

	public Label getPeriodFromNote() {
		return periodFromNote;
	}

	public Label getPeriodToNote() {
		return periodToNote;
	}

	public Label getLatitudeLabel() {
		return latitudeLabel;
	}

	public Label getLatitudeFromLabel() {
		return latitudeFromLabel;
	}

	public Text getLatitudeFromText() {
		return latitudeFromText;
	}

	public Label getLatitudeFromNote() {
		return latitudeFromNote;
	}

	public Label getLatitudeToLabel() {
		return latitudeToLabel;
	}

	public Text getLatitudeToText() {
		return latitudeToText;
	}

	public Label getLatitudeToNote() {
		return latitudeToNote;
	}

	public Label getLongitudeLabel() {
		return longitudeLabel;
	}

	public Label getLongitudeFromLabel() {
		return longitudeFromLabel;
	}

	public Text getLongitudeFromText() {
		return longitudeFromText;
	}

	public Label getLongitudeFromNote() {
		return longitudeFromNote;
	}

	public Label getLongitudeToLabel() {
		return longitudeToLabel;
	}

	public Text getLongitudeToText() {
		return longitudeToText;
	}

	public Label getLongitudeToNote() {
		return longitudeToNote;
	}

	public Label getMinimumMagnitudeLabel() {
		return minimumMagnitudeLabel;
	}

	public Text getMinimumMagnitudeText() {
		return minimumMagnitudeText;
	}

	public Button getRestrictButton() {
		return restrictButton;
	}

	public Label getOutputFormatLabel() {
		return outputFormatLabel;
	}

	public Composite getRadioComposite() {
		return radioComposite;
	}

	public Map<Format, Button> getFormatRadios() {
		return formatRadios;
	}

	public Label getResultsLabel() {
		return resultsLabel;
	}

	public Text getResultsText() {
		return resultsText;
	}

	public Group getCriteriaGroup() {
		return criteriaGroup;
	}

	public Composite getButtonsComposite() {
		return buttonsComposite;
	}

	public Button getSearchButton() {
		return searchButton;
	}

	public Label getResultsNote() {
		return resultsNote;
	}

	public Button getAutoRefreshButton() {
		return autoRefreshButton;
	}

	public Text getAutoRefreshText() {
		return autoRefreshText;
	}

	public Button getStopButton() {
		return stopButton;
	}

	public Button getClearButton() {
		return clearButton;
	}

	public Button getOpenMap() {
		return openMap;
	}

	public MapBoundsDialog getMapBoundsDialog() {
		return mapBoundsDialog;
	}

}