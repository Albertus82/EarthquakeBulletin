package gov.usgs.cr.hazards.feregion.fe_1995;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.usgs.cr.hazards.feregion.fe_1995.Coordinates.IllegalCoordinateException;
import it.albertus.util.logging.LoggerFactory;

/**
 * @author Bob Simpson
 */
public class FERegion {

	private static final Logger logger = LoggerFactory.getLogger(FERegion.class);

	private final FECache cache;

	/**
	 * Initializes a internal cache. Reuse the instance whenever possible.
	 */
	public FERegion() throws IOException {
		cache = new FECache();
	}

	public Region getRegion(final Coordinates coordinates) {
		final int fenum = getNumber(coordinates);
		final String fename = cache.getNames().get(fenum - 1);
		logger.log(Level.FINE, "{0} {1}", new Object[] { fenum, fename });

		return new Region(fenum, fename);
	}

	private int getNumber(final Coordinates coordinates) {
		// Take absolute values & truncate to integers...
		final int lt = (int) Math.abs(coordinates.getLatitude());
		final int ln = (int) Math.abs(coordinates.getLongitude());

		// Get quadrant
		final String quad;
		if (coordinates.getLatitude() >= 0.0) {
			quad = coordinates.getLongitude() >= 0.0 ? "ne" : "nw";
		}
		else {
			quad = coordinates.getLongitude() >= 0.0 ? "se" : "sw";
		}
		logger.log(Level.FINE, " * quad, lt, ln  = {0} {1} {2}", new Object[] { quad, lt, ln });

		// Find location of the latitude tier in the appropriate quadrant file.
		final int beg = cache.getLatbegins().get(quad).get(lt); // Location of first item for latitude lt.
		final int num = cache.getLonsperlat().get(quad).get(lt); // Number of items for latitude lt.
		logger.log(Level.FINE, " * beg = {0} num = {1}", new int[] { beg, num });

		// Extract this tier of longitude and f-e numbers for latitude lt.
		final List<Integer> mylons = cache.getLons().get(quad).subList(beg, beg + num);
		final List<Integer> myfenums = cache.getFenums().get(quad).subList(beg, beg + num);
		logger.log(Level.FINE, "mylons: {0}", mylons);
		logger.log(Level.FINE, "myfenums: {0}", myfenums);

		int n = 0;
		for (final int item : mylons) {
			if (item > ln) {
				break;
			}
			n++;
		}

		final int feindex = n - 1;
		final int fenum = myfenums.get(feindex);
		logger.log(Level.FINE, "{0} {1} {2}", new int[] { n, feindex, fenum });

		return fenum;
	}

	/**
	 * Returns Flinn-Engdahl Region name from decimal lon,lat values given on
	 * command line.
	 * 
	 * Version 0.2 - Bob Simpson January, 2003 <simpson@usgs.gov>
	 * 
	 * With fix supplied by George Randall <ger@geophysics.lanl.gov> 2003-02-03
	 */
	public static void main(final String[] args) throws IOException {
		if (args.length != 2) {
			System.err.println("   Usage:  feregion  <lon> <lat>");
			System.err.println("   As In:  feregion  -122.5  36.2");
			System.err.println("   As In:  feregion   122.5W 36.2N");
		}
		else {
			final FERegion instance = new FERegion();
			try {
				System.out.println(instance.getRegion(Coordinates.parse(args[0], args[1])).getName(true));
			}
			catch (final IllegalCoordinateException e) {
				System.err.println(e.getMessage());
				logger.log(Level.FINE, e.toString(), e);
				System.exit(1);
			}
		}
	}

}
