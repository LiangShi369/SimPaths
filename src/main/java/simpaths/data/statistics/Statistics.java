package simpaths.data.statistics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import microsim.data.db.PanelEntityKey;

@Entity
public class Statistics {

	@Id
	private PanelEntityKey key = new PanelEntityKey(1L);

	@Column(name = "Gini_coefficient_individual_market_income_nationally")
	private double statYMktNatGini;

	@Column(name = "Gini_coefficient_equivalised_household_disposable_income_nationally")
	private double statYHhDispEquivNatGini;

	@Column(name = "Median_equivalised_household_disposable_income")
	private double yHhDispEquivP50;
	
	//Percentiles of ydses:
	@Column(name = "Ydses_p20")
	private double yHhQuintilesC5P20;
	
	@Column(name = "Ydses_p40")
	private double yHhQuintilesC5P40;
	
	@Column(name = "Ydses_p60")
	private double yHhQuintilesC5P60;
	
	@Column(name = "Ydses_p80")
	private double yHhQuintilesC5P80;

	//Percentiles of gross labour income:
	@Column(name = "Gross_Labour_Force_Earnings_p20")
	private double yLabFceEarningsP20;

	@Column(name = "Gross_Labour_Force_Earnings_p40")
	private double yLabFceEarningsP40;

	@Column(name = "Gross_Labour_Force_Earnings_p60")
	private double yLabFceEarningsP60;

	@Column(name = "Gross_Labour_Force_Earnings_p80")
	private double yLabFceEarningsP80;

	@Column(name = "Gross_Employed_Earnings_p20")
	private double yEmployedEarningsP20;

	@Column(name = "Gross_Employed_Earnings_p40")
	private double yEmployedEarningsP40;

	@Column(name = "Gross_Employed_Earnings_p60")
	private double yEmployedEarningsP60;

	@Column(name = "Gross_Employed_Earnings_p80")
	private double yEmployedEarningsP80;

	//Equivalised disposable income
	@Column(name = "EDI_p50")
	private double edi_p50;

	//Percentiles of SIndex:
	@Column(name = "SIndex_p50")
	private double sIndex_p50;

	////	Risk-of-poverty threshold is set at 60% of the national median equivalised household disposable income.
//	@Column(name = "Risk_of_poverty_threshold")
//	private double riskOfPovertyThreshold;
	
	public void setGiniPersonalGrossEarningsNational(double statYMktNatGini) {
		this.statYMktNatGini = statYMktNatGini;
	}
	
	public void setGiniEquivalisedHouseholdDisposableIncomeNational(double statYHhDispEquivNatGini) {
		this.statYHhDispEquivNatGini = statYHhDispEquivNatGini;
	}

	public double getMedianEquivalisedHouseholdDisposableIncome() {
		return yHhDispEquivP50;
	}

	public void setMedianEquivalisedHouseholdDisposableIncome(double yHhDispEquivP50) {
		this.yHhDispEquivP50 = yHhDispEquivP50;
	}
	
	public double getYHhQuintilesC5P20() {
		return yHhQuintilesC5P20;
	}

	public void setYHhQuintilesC5P20(double yHhQuintilesC5P20) {
		this.yHhQuintilesC5P20 = yHhQuintilesC5P20;
	}

	public double getYHhQuintilesC5P40() {
		return yHhQuintilesC5P40;
	}

	public void setYHhQuintilesC5P40(double yHhQuintilesC5P40) {
		this.yHhQuintilesC5P40 = yHhQuintilesC5P40;
	}

	public double getYHhQuintilesC5P60() {
		return yHhQuintilesC5P60;
	}

	public void setYHhQuintilesC5P60(double yHhQuintilesC5P60) {
		this.yHhQuintilesC5P60 = yHhQuintilesC5P60;
	}

	public double getYHhQuintilesC5P80() {
		return yHhQuintilesC5P80;
	}

	public void setYHhQuintilesC5P80(double yHhQuintilesC5P80) {
		this.yHhQuintilesC5P80 = yHhQuintilesC5P80;
	}

	public double getSIndex_p50() {
		return sIndex_p50;
	}

	public void setSIndex_p50(double sIndex_p50) {
		this.sIndex_p50 = sIndex_p50;
	}

	public double getGrossLabourForceEarnings_p20() {
		return yLabFceEarningsP20;
	}

	public void setGrossLabourForceEarnings_p20(double yLabP20) { this.yLabFceEarningsP20 = yLabP20; }

	public double getGrossLabourForceEarnings_p40() {
		return yLabFceEarningsP40;
	}

	public void setGrossLabourForceEarnings_p40(double yLabP40) {
		this.yLabFceEarningsP40 = yLabP40;
	}

	public double getGrossLabourForceEarnings_p60() {
		return yLabFceEarningsP60;
	}

	public void setGrossLabourForceEarnings_p60(double yLabP60) {
		this.yLabFceEarningsP60 = yLabP60;
	}

	public double getGrossLabourForceEarnings_p80() { return yLabFceEarningsP80; }

	public void setGrossLabourForceEarnings_p80(double yLabP80) { this.yLabFceEarningsP80 = yLabP80; }

	public double getEmployedEarningsP20() { return yEmployedEarningsP20; }

	public void setEmployedEarningsP20(double yEmployedEarningsP20) { this.yEmployedEarningsP20 = yEmployedEarningsP20; }

	public double getEmployedEarningsP40() { return yEmployedEarningsP40; }

	public void setEmployedEarningsP40(double yEmployedEarningsP40) { this.yEmployedEarningsP40 = yEmployedEarningsP40; }

	public double getEmployedEarningsP60() {
		return yEmployedEarningsP60;
	}

	public void setEmployedEarningsP60(double yEmployedEarningsP60) {
		this.yEmployedEarningsP60 = yEmployedEarningsP60;
	}

	public double getEmployedEarningsP80() {
		return yEmployedEarningsP80;
	}

	public void setEmployedEarningsP80(double yEmployedEarningsP80) {
		this.yEmployedEarningsP80 = yEmployedEarningsP80;
	}

	public double getEdi_p50() {
		return edi_p50;
	}

	public void setEdi_p50(double edi_p50) {
		this.edi_p50 = edi_p50;
	}

//	public double getRiskOfPovertyThreshold() {
//		return riskOfPovertyThreshold;
//	}
//
//	public void setRiskOfPovertyThreshold(double riskOfPovertyThreshold) {
//		this.riskOfPovertyThreshold = riskOfPovertyThreshold;
//	}

}
