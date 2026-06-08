package simpaths.data.statistics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import microsim.data.db.PanelEntityKey;
import simpaths.data.Parameters;
import simpaths.model.BenefitUnit;
import simpaths.model.Person;
import simpaths.model.SimPathsModel;
import simpaths.model.enums.Education;
import simpaths.model.enums.Indicator;

@Entity
public class Statistics3 {

    @Id
    private PanelEntityKey key = new PanelEntityKey(1L);

    //population shares contributing to private pensions by age group
    @Column(name = "pr_op_18_29")
    private double yOPMember18to29Share;

    @Column(name = "pr_op_30_54")
    private double yOPMember30to54Share;

    @Column(name = "pr_op_55_74")
    private double yOPMember55to74Share;

    @Column(name = "pr_pp_18_29")
    private double yPPMember18to29Share;

    @Column(name = "pr_pp_30_54")
    private double yPPMember30to54Share;

    @Column(name = "pr_pp_55_74")
    private double yPPMember55to74Share;

    //pension contribution rates of members
    @Column(name = "avcr_op_ee")
    private double yContRateOPEeAvg;

    @Column(name = "avcr_op_er")
    private double yContRateOPErAvg;

    @Column(name = "avcr_pp")
    private double yContRatePPAvg;

    //pension wealth by age group
    @Column(name = "av_pens_wealth_18_29")
    private double yWealthPensValue18to29Avg;

    @Column(name = "av_pens_wealth_30_54")
    private double yWealthPensValue30to54Avg;

    @Column(name = "av_pens_wealth_55_74")
    private double yWealthPensValue55to74Avg;

    //non-pension wealth by age group
    @Column(name = "av_nonpens_wealth_18_29")
    private double yWealthNonPensValue18to29Avg;

    @Column(name = "av_nonpens_wealth_30_54")
    private double yWealthNonPensValue30to54Avg;

    @Column(name = "av_nonpens_wealth_55_74")
    private double yWealthNonPensValue55to74Avg;

    //benefit unit non-pension wealth by age group of reference person
    @Column(name = "av_bu_nonpens_wealth_18_29")
    private double yWealthBUNonPensValue18to29Avg;

    @Column(name = "av_bu_nonpens_wealth_30_54")
    private double yWealthBUNonPensValue30to54Avg;

    @Column(name = "av_bu_nonpens_wealth_55_74")
    private double yWealthBUNonPensValue55to74Avg;

    //benefit unit housing wealth by age group of reference person
    @Column(name = "av_bu_housing_wealth_18_29")
    private double yWealthBUHousingValue18to29Avg;

    @Column(name = "av_bu_housing_wealth_30_54")
    private double yWealthBUHousingValue30to54Avg;

    @Column(name = "av_bu_housing_wealth_55_74")
    private double yWealthBUHousingValue55to74Avg;

    //benefit unit mortgage by age group of reference person
    @Column(name = "av_bu_mortgage_wealth_18_29")
    private double yWealthBUMortgageValue18to29Avg;

    @Column(name = "av_bu_mortgage_wealth_30_54")
    private double yWealthBUMortgageValue30to54Avg;

    @Column(name = "av_bu_mortgage_wealth_55_74")
    private double yWealthBUMortgageValue55to74Avg;

    //benefit unit incidence of housing by age group of reference person
    @Column(name = "av_bu_home_owner_18_29")
    private double yWealthBUHomeOwner18to29Avg;

    @Column(name = "av_bu_home_owner_30_54")
    private double yWealthBUHomeOwner30to54Avg;

    @Column(name = "av_bu_home_owner_55_74")
    private double yWealthBUHomeOwner55to74Avg;

    //benefit unit incidence of mortgages by age group of reference person
    @Column(name = "av_bu_mortgage_holder_18_29")
    private double yWealthBUMortgageHolder18to29Avg;

    @Column(name = "av_bu_mortgage_holder_30_54")
    private double yWealthBUMortgageHolder30to54Avg;

    @Column(name = "av_bu_mortgage_holder_55_74")
    private double yWealthBUMortgageHolder55to74Avg;

    public double getyWealthBUMortgageHolder18to29Avg() {
        return yWealthBUMortgageHolder18to29Avg;
    }

    public void setyWealthBUMortgageHolder18to29Avg(double yWealthBUMortgageHolder18to29Avg) {
        this.yWealthBUMortgageHolder18to29Avg = yWealthBUMortgageHolder18to29Avg;
    }

    public double getyWealthBUMortgageHolder30to54Avg() {
        return yWealthBUMortgageHolder30to54Avg;
    }

    public void setyWealthBUMortgageHolder30to54Avg(double yWealthBUMortgageHolder30to54Avg) {
        this.yWealthBUMortgageHolder30to54Avg = yWealthBUMortgageHolder30to54Avg;
    }

    public double getyWealthBUMortgageHolder55to74Avg() {
        return yWealthBUMortgageHolder55to74Avg;
    }

    public void setyWealthBUMortgageHolder55to74Avg(double yWealthBUMortgageHolder55to74Avg) {
        this.yWealthBUMortgageHolder55to74Avg = yWealthBUMortgageHolder55to74Avg;
    }

    public double getyWealthBUHomeOwner18to29Avg() {
        return yWealthBUHomeOwner18to29Avg;
    }

    public void setyWealthBUHomeOwner18to29Avg(double yWealthBUHomeOwner18to29Avg) {
        this.yWealthBUHomeOwner18to29Avg = yWealthBUHomeOwner18to29Avg;
    }

    public double getyWealthBUHomeOwner30to54Avg() {
        return yWealthBUHomeOwner30to54Avg;
    }

    public void setyWealthBUHomeOwner30to54Avg(double yWealthBUHomeOwner30to54Avg) {
        this.yWealthBUHomeOwner30to54Avg = yWealthBUHomeOwner30to54Avg;
    }

    public double getyWealthBUHomeOwner55to74Avg() {
        return yWealthBUHomeOwner55to74Avg;
    }

    public void setyWealthBUHomeOwner55to74Avg(double yWealthBUHomeOwner55to74Avg) {
        this.yWealthBUHomeOwner55to74Avg = yWealthBUHomeOwner55to74Avg;
    }

    public double getyWealthBUHousingValue18to29Avg() {
        return yWealthBUHousingValue18to29Avg;
    }

    public void setyWealthBUHousingValue18to29Avg(double yWealthBUHousingValue18to29Avg) {
        this.yWealthBUHousingValue18to29Avg = yWealthBUHousingValue18to29Avg;
    }

    public double getyWealthBUHousingValue30to54Avg() {
        return yWealthBUHousingValue30to54Avg;
    }

    public void setyWealthBUHousingValue30to54Avg(double yWealthBUHousingValue30to54Avg) {
        this.yWealthBUHousingValue30to54Avg = yWealthBUHousingValue30to54Avg;
    }

    public double getyWealthBUHousingValue55to74Avg() {
        return yWealthBUHousingValue55to74Avg;
    }

    public void setyWealthBUHousingValue55to74Avg(double yWealthBUHousingValue55to74Avg) {
        this.yWealthBUHousingValue55to74Avg = yWealthBUHousingValue55to74Avg;
    }

    public double getyWealthBUMortgageValue18to29Avg() {
        return yWealthBUMortgageValue18to29Avg;
    }

    public void setyWealthBUMortgageValue18to29Avg(double yWealthBUMortgageValue18to29Avg) {
        this.yWealthBUMortgageValue18to29Avg = yWealthBUMortgageValue18to29Avg;
    }

    public double getyWealthBUMortgageValue30to54Avg() {
        return yWealthBUMortgageValue30to54Avg;
    }

    public void setyWealthBUMortgageValue30to54Avg(double yWealthBUMortgageValue30to54Avg) {
        this.yWealthBUMortgageValue30to54Avg = yWealthBUMortgageValue30to54Avg;
    }

    public double getyWealthBUMortgageValue55to74Avg() {
        return yWealthBUMortgageValue55to74Avg;
    }

    public void setyWealthBUMortgageValue55to74Avg(double yWealthBUMortgageValue55to74Avg) {
        this.yWealthBUMortgageValue55to74Avg = yWealthBUMortgageValue55to74Avg;
    }

    public double getyWealthBUNonPensValue18to29Avg() {
        return yWealthBUNonPensValue18to29Avg;
    }

    public void setyWealthBUNonPensValue18to29Avg(double yWealthBUNonPensValue18to29Avg) {
        this.yWealthBUNonPensValue18to29Avg = yWealthBUNonPensValue18to29Avg;
    }

    public double getyWealthBUNonPensValue30to54Avg() {
        return yWealthBUNonPensValue30to54Avg;
    }

    public void setyWealthBUNonPensValue30to54Avg(double yWealthBUNonPensValue30to54Avg) {
        this.yWealthBUNonPensValue30to54Avg = yWealthBUNonPensValue30to54Avg;
    }

    public double getyWealthBUNonPensValue55to74Avg() {
        return yWealthBUNonPensValue55to74Avg;
    }

    public void setyWealthBUNonPensValue55to74Avg(double yWealthBUNonPensValue55to74Avg) {
        this.yWealthBUNonPensValue55to74Avg = yWealthBUNonPensValue55to74Avg;
    }

    public PanelEntityKey getKey() {
        return key;
    }

    public void setKey(PanelEntityKey key) {
        this.key = key;
    }

    public double getyWealthNonPensValue18to29Avg() {
        return yWealthNonPensValue18to29Avg;
    }

    public void setyWealthNonPensValue18to29Avg(double yWealthNonPensValue18to29Avg) {
        this.yWealthNonPensValue18to29Avg = yWealthNonPensValue18to29Avg;
    }

    public double getyWealthNonPensValue30to54Avg() {
        return yWealthNonPensValue30to54Avg;
    }

    public void setyWealthNonPensValue30to54Avg(double yWealthNonPensValue30to54Avg) {
        this.yWealthNonPensValue30to54Avg = yWealthNonPensValue30to54Avg;
    }

    public double getyWealthNonPensValue55to74Avg() {
        return yWealthNonPensValue55to74Avg;
    }

    public void setyWealthNonPensValue55to74Avg(double yWealthNonPensValue55to74Avg) {
        this.yWealthNonPensValue55to74Avg = yWealthNonPensValue55to74Avg;
    }

    public double getyOPMember18to29Share() {
        return yOPMember18to29Share;
    }

    public void setyOPMember18to29Share(double yOPMember18to29Share) {
        this.yOPMember18to29Share = yOPMember18to29Share;
    }

    public double getyOPMember30to54Share() {
        return yOPMember30to54Share;
    }

    public void setyOPMember30to54Share(double yOPMember30to54Share) {
        this.yOPMember30to54Share = yOPMember30to54Share;
    }

    public double getyOPMember55to74Share() {
        return yOPMember55to74Share;
    }

    public void setyOPMember55to74Share(double yOPMember55to74Share) {
        this.yOPMember55to74Share = yOPMember55to74Share;
    }

    public double getyPPMember18to29Share() {
        return yPPMember18to29Share;
    }

    public void setyPPMember18to29Share(double yPPMember18to29Share) {
        this.yPPMember18to29Share = yPPMember18to29Share;
    }

    public double getyPPMember30to54Share() {
        return yPPMember30to54Share;
    }

    public void setyPPMember30to54Share(double yPPMember30to54Share) {
        this.yPPMember30to54Share = yPPMember30to54Share;
    }

    public double getyPPMember55to74Share() {
        return yPPMember55to74Share;
    }

    public void setyPPMember55to74Share(double yPPMember55to74Share) {
        this.yPPMember55to74Share = yPPMember55to74Share;
    }

    public double getyContRateOPEeAvg() {
        return yContRateOPEeAvg;
    }

    public void setyContRateOPEeAvg(double yContRateOPEeAvg) {
        this.yContRateOPEeAvg = yContRateOPEeAvg;
    }

    public double getyContRateOPErAvg() {
        return yContRateOPErAvg;
    }

    public void setyContRateOPErAvg(double yContRateOPErAvg) {
        this.yContRateOPErAvg = yContRateOPErAvg;
    }

    public double getyContRatePPAvg() {
        return yContRatePPAvg;
    }

    public void setyContRatePPAvg(double yContRatePPAvg) {
        this.yContRatePPAvg = yContRatePPAvg;
    }

    public double getyWealthPensValue18to29Avg() {
        return yWealthPensValue18to29Avg;
    }

    public void setyWealthPensValue18to29Avg(double yWealthPensValue18to29Avg) {
        this.yWealthPensValue18to29Avg = yWealthPensValue18to29Avg;
    }

    public double getyWealthPensValue30to54Avg() {
        return yWealthPensValue30to54Avg;
    }

    public void setyWealthPensValue30to54Avg(double yWealthPensValue30to54Avg) {
        this.yWealthPensValue30to54Avg = yWealthPensValue30to54Avg;
    }

    public double getyWealthPensValue55to74Avg() {
        return yWealthPensValue55to74Avg;
    }

    public void setyWealthPensValue55to74Avg(double yWealthPensValue55to74Avg) {
        this.yWealthPensValue55to74Avg = yWealthPensValue55to74Avg;
    }

    public void update(SimPathsModel model) {

        // initialise outputs
        double[] prOPMemb = {0.,0.,0.};
        double[] prPPMemb = {0.,0.,0.};
        double avContRateOPEe = 0.;
        double avContRateOPEr = 0.;
        double avContRatePP = 0.;
        double[] avPensWealth = {0.,0.,0.};
        double[] avNonPensWealth = {0.,0.,0.};
        double[] popula = {0.,0.,0.};

        // calculate statistics
        for (Person person : model.getPersons()) {
            // loop over entire population

            int ii = -1;
            if (person.getDemAge()>=18 && person.getDemAge()<=29) {
                ii = 0;
            } else if (person.getDemAge()>=30 && person.getDemAge()<=54) {
                ii = 1;
            } else if (person.getDemAge()>=55 && person.getDemAge()<=74) {
                ii = 2;
            }
            if (ii>=0) {

                double crOPEe = person.getContRateOPEe(false);
                double crOPEr = person.getContRateOPEr(false);
                double crPP = person.getContRatePP(false);
                double pw = person.getWealthPensValue(false);
                double npw = person.getWealthNonPensValue(false);

                prOPMemb[ii] += (crOPEe + crOPEr > 0.0) ? 1.0: 0.0;
                prPPMemb[ii] += (crPP > 0.0) ? 1.0: 0.0;
                avContRateOPEe += crOPEe;
                avContRateOPEr += crOPEr;
                avContRatePP += crPP;
                avPensWealth[ii] += pw;
                avNonPensWealth[ii] += npw;
                popula[ii] += 1.0;
            }
        }
        if (prOPMemb[0]+prOPMemb[1]+prOPMemb[2]>0) {

            avContRateOPEe /= (prOPMemb[0]+prOPMemb[1]+prOPMemb[2]);
            avContRateOPEr /= (prOPMemb[0]+prOPMemb[1]+prOPMemb[2]);
        }
        if (prPPMemb[0]+prPPMemb[1]+prPPMemb[2]>0)
            avContRatePP /= (prPPMemb[0]+prPPMemb[1]+prPPMemb[2]);
        for (int ii=0; ii<=2; ii++) {

            if (popula[ii] > 0) {

                prOPMemb[ii] /= popula[ii];
                prPPMemb[ii] /= popula[ii];
                avPensWealth[ii] /= popula[ii];
                avNonPensWealth[ii] /= popula[ii];
            }
        }

        double[] avBUNPV = {0.,0.,0.};
        double[] avBUHgV = {0.,0.,0.};
        double[] avBUMgV = {0.,0.,0.};
        double[] prBUHgV = {0.,0.,0.};
        double[] prBUMgV = {0.,0.,0.};
        double[] popBU = {0.,0.,0.};
        for (BenefitUnit bu : model.getBenefitUnits()) {

            int ii = -1;
            if (bu.getRefPerson().getDemAge()>=18 && bu.getRefPerson().getDemAge()<=29) {
                ii = 0;
            } else if (bu.getRefPerson().getDemAge()>=30 && bu.getRefPerson().getDemAge()<=54) {
                ii = 1;
            } else if (bu.getRefPerson().getDemAge()>=55 && bu.getRefPerson().getDemAge()<=74) {
                ii = 2;
            }
            if (ii>=0) {

                avBUNPV[ii] += bu.getWealthNonPensValue();
                avBUHgV[ii] += bu.getWealthPrptyValue();
                avBUMgV[ii] += bu.getWealthNonPension().getWealthMortgageDebtValue();
                prBUHgV[ii] += bu.getWealthPrptyValue() > 0.0 ? 1.0: 0.0;
                prBUMgV[ii] += bu.getWealthNonPension().getWealthMortgageDebtValue() > 0.0 ? 1.0: 0.0;
                popBU[ii] += 1.0;
            }
        }
        for (int ii=0; ii<=2; ii++) {

            if (popBU[ii] > 0) {

                avBUNPV[ii] /= popBU[ii];
                avBUHgV[ii] /= popBU[ii];
                avBUMgV[ii] /= popBU[ii];
                prBUHgV[ii] /= popBU[ii];
                prBUMgV[ii] /= popBU[ii];
            }
        }

        // populate outputs
        setyOPMember18to29Share(prOPMemb[0]);
        setyOPMember30to54Share(prOPMemb[1]);
        setyOPMember55to74Share(prOPMemb[2]);
        setyPPMember18to29Share(prPPMemb[0]);
        setyPPMember30to54Share(prPPMemb[1]);
        setyPPMember55to74Share(prPPMemb[2]);
        setyContRateOPEeAvg(avContRateOPEe);
        setyContRateOPErAvg(avContRateOPEr);
        setyContRatePPAvg(avContRatePP);
        setyWealthPensValue18to29Avg(avPensWealth[0]);
        setyWealthPensValue30to54Avg(avPensWealth[1]);
        setyWealthPensValue55to74Avg(avPensWealth[2]);
        setyWealthNonPensValue18to29Avg(avNonPensWealth[0]);
        setyWealthNonPensValue30to54Avg(avNonPensWealth[1]);
        setyWealthNonPensValue55to74Avg(avNonPensWealth[2]);
        setyWealthBUNonPensValue18to29Avg(avBUNPV[0]);
        setyWealthBUNonPensValue30to54Avg(avBUNPV[1]);
        setyWealthBUNonPensValue55to74Avg(avBUNPV[2]);
        setyWealthBUHousingValue18to29Avg(avBUHgV[0]);
        setyWealthBUHousingValue30to54Avg(avBUHgV[1]);
        setyWealthBUHousingValue55to74Avg(avBUHgV[2]);
        setyWealthBUMortgageValue18to29Avg(avBUMgV[0]);
        setyWealthBUMortgageValue30to54Avg(avBUMgV[1]);
        setyWealthBUMortgageValue55to74Avg(avBUMgV[2]);
        setyWealthBUHomeOwner18to29Avg(prBUHgV[0]);
        setyWealthBUHomeOwner30to54Avg(prBUHgV[1]);
        setyWealthBUHomeOwner55to74Avg(prBUHgV[2]);
        setyWealthBUMortgageHolder18to29Avg(prBUMgV[0]);
        setyWealthBUMortgageHolder30to54Avg(prBUMgV[1]);
        setyWealthBUMortgageHolder55to74Avg(prBUMgV[2]);
    }
}
