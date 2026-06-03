package simpaths.data.statistics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import microsim.data.db.PanelEntityKey;
import simpaths.data.Parameters;
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

    public PanelEntityKey getKey() {
        return key;
    }

    public void setKey(PanelEntityKey key) {
        this.key = key;
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
        double[] avValue = {0.,0.,0.};
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

                prOPMemb[ii] += (crOPEe + crOPEr > 0.0) ? 1.0: 0.0;
                prPPMemb[ii] += (crPP > 0.0) ? 1.0: 0.0;
                avContRateOPEe += crOPEe;
                avContRateOPEr += crOPEr;
                avContRatePP += crPP;
                avValue[ii] += pw;
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
                avValue[ii] /= popula[ii];
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
        setyWealthPensValue18to29Avg(avValue[0]);
        setyWealthPensValue30to54Avg(avValue[1]);
        setyWealthPensValue55to74Avg(avValue[2]);
    }
}
