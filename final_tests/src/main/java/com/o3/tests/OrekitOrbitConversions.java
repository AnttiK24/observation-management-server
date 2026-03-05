package com.o3.tests;

import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

import com.o3.tests.messages.OrbitalElements;
import com.o3.tests.messages.StateVector;

public final class OrekitOrbitConversions {

	private OrekitOrbitConversions() {
	}

	/**
	 * Convert StateVector (AU, AU/day) to OrbitalElements (AU, deg, deg).
	 */
	public static OrbitalElements stateVectorToOrbitalElements(StateVector stateVector) {
		// Convert AU -> m and AU/day -> m/s
		Vector3D position = new Vector3D(
				stateVector.getPositionAu().get(0) * Constants.JPL_SSD_ASTRONOMICAL_UNIT,
				stateVector.getPositionAu().get(1) * Constants.JPL_SSD_ASTRONOMICAL_UNIT,
				stateVector.getPositionAu().get(2) * Constants.JPL_SSD_ASTRONOMICAL_UNIT);

		Vector3D velocity = new Vector3D(
				stateVector.getVelocityAuPerDay().get(0) * Constants.JPL_SSD_ASTRONOMICAL_UNIT
						/ Constants.JULIAN_DAY,
				stateVector.getVelocityAuPerDay().get(1) * Constants.JPL_SSD_ASTRONOMICAL_UNIT
						/ Constants.JULIAN_DAY,
				stateVector.getVelocityAuPerDay().get(2) * Constants.JPL_SSD_ASTRONOMICAL_UNIT
						/ Constants.JULIAN_DAY);

		PVCoordinates pv = new PVCoordinates(position, velocity);

		Orbit cart = new CartesianOrbit(
				pv,
				FramesFactory.getGCRF(),
				AbsoluteDate.J2000_EPOCH,
				Constants.IAU_2015_NOMINAL_SUN_GM);

		// Convert Cartesian to Keplerian using Orekit
		KeplerianOrbit kepler = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(cart);

		double aAu = kepler.getA() / Constants.JPL_SSD_ASTRONOMICAL_UNIT;
		double e = kepler.getE();
		double iDeg = FastMath.toDegrees(kepler.getI());
		double raanDeg = FastMath.toDegrees(kepler.getRightAscensionOfAscendingNode());
		double argPerDeg = FastMath.toDegrees(kepler.getPerigeeArgument());
		double meanAnomalyDeg = FastMath.toDegrees(kepler.getMeanAnomaly());

		return new OrbitalElements(
				aAu,
				e,
				normalizeDeg(iDeg),
				normalizeDeg(raanDeg),
				normalizeDeg(argPerDeg),
				normalizeDeg(meanAnomalyDeg));
	}

	/**
	 * Convert OrbitalElements (AU, deg, deg) to StateVector (AU, AU/day).
	 */
	public static StateVector orbitalElementsToStateVector(OrbitalElements orbitalElements) {
		double semiMajorAxis = orbitalElements.getSemiMajorAxisAu() * Constants.JPL_SSD_ASTRONOMICAL_UNIT;
		double eccentricity = orbitalElements.getEccentricity();

		double inclinationRad = FastMath.toRadians(orbitalElements.getInclinationDeg());
		double raanRad = FastMath.toRadians(orbitalElements.getLongitudeAscendingNodeDeg());
		double perigeeArgRad = FastMath.toRadians(orbitalElements.getArgumentOfPeriapsisDeg());
		double meanAnomalyRad = FastMath.toRadians(orbitalElements.getMeanAnomalyDeg());

		// Build KeplerianOrbit from mean anomaly
		KeplerianOrbit kepler = new KeplerianOrbit(
				semiMajorAxis,
				eccentricity,
				inclinationRad,
				perigeeArgRad,
				raanRad,
				meanAnomalyRad,
				PositionAngleType.MEAN,
				FramesFactory.getGCRF(),
				AbsoluteDate.J2000_EPOCH,
				Constants.IAU_2015_NOMINAL_SUN_GM);

		PVCoordinates pv = kepler.getPVCoordinates();

		Vector3D position = pv.getPosition();
		Vector3D velocity = pv.getVelocity();

		// Convert back to AU and AU/day
		List<Double> positionAu = List.of(
				position.getX() / Constants.JPL_SSD_ASTRONOMICAL_UNIT,
				position.getY() / Constants.JPL_SSD_ASTRONOMICAL_UNIT,
				position.getZ() / Constants.JPL_SSD_ASTRONOMICAL_UNIT);

		List<Double> velocityAuPerDay = List.of(
				velocity.getX() * Constants.JULIAN_DAY / Constants.JPL_SSD_ASTRONOMICAL_UNIT,
				velocity.getY() * Constants.JULIAN_DAY / Constants.JPL_SSD_ASTRONOMICAL_UNIT,
				velocity.getZ() * Constants.JULIAN_DAY / Constants.JPL_SSD_ASTRONOMICAL_UNIT);

		return new StateVector(positionAu, velocityAuPerDay);
	}

	/**
	 * Normalize degree angles to 0..360 degrees
	 *
	 * @param deg Original degrees
	 * @return Normalized degrees
	 */
	private static double normalizeDeg(double deg) {
		double x = deg % 360.0;
		if (x < 0)
			x += 360.0;
		return x;
	}
}
