package it.albertus.eqbulletin.service.decode.html;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import it.albertus.eqbulletin.resources.Messages;

public enum HtmlBulletinVersion {

	OLD,
	NEW;

	/**
	 * @see <a href=
	 *      "https://geofon.gfz-potsdam.de/forum/t/switching-over-geofon-web-site-today/3616">Switching
	 *      over GEOFON web site today</a>
	 */
	private static final ZonedDateTime SWITCH_DATETIME = ZonedDateTime.of(2019, 10, 8, 10, 0, 0, 0, ZoneId.of("UTC"));

	public static final HtmlBulletinVersion DEFAULT = OLD;

	public String getLabel() {
		return Messages.get("lbl.html.bulletin.version." + name().toLowerCase(), DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Messages.getLanguage().getLocale()).format(SWITCH_DATETIME));
	}

	public static HtmlBulletinVersion forValue(final String value) {
		if (value != null) {
			for (final HtmlBulletinVersion version : values()) {
				if (value.trim().equalsIgnoreCase(version.name())) {
					return version;
				}
			}
		}
		return DEFAULT;
	}

}