package it.albertus.eqbulletin.service.geofon.html;

import java.util.ArrayList;
import java.util.List;

public class TableData {

	private final List<String> items = new ArrayList<>();

	public List<String> getItems() {
		return items;
	}

	public void addItem(final String item) {
		items.add(item);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + items.hashCode();
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof TableData)) {
			return false;
		}
		final TableData other = (TableData) obj;
		return items.equals(other.items);
	}

	@Override
	public String toString() {
		return "TableData [items=" + items + "]";
	}

}
