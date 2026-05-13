package simpaths.model.person;

import simpaths.data.ManagerRegressions;
import simpaths.data.Parameters;
import simpaths.data.RegressionName;
import simpaths.model.Person;
import simpaths.model.enums.Les_c4;
import simpaths.model.enums.PensionContribRate;

public class PrivatePension {

    private boolean memberPP;                            //indicator that is active member of personal pension in current year
    private boolean memberOP;                            //indicator that is active member of occupational pension in current year
    private double contRateOPEe;                         //contribution rate of employee to occupational pension
    private double contRateOPEr;                         //contribution rate of employer to occupational pension
    private double contRatePP;                           //contribution rate to personal pension
    private double wealth;                               //wealth held in private pensions

    public PrivatePension() {
        memberPP = false;
        memberOP = false;
        contRateOPEr = 0.0;
        contRateOPEe = 0.0;
        contRatePP = 0.0;
        wealth = 0.0;
    }

    public PrivatePension(PrivatePension original) {
        this.memberPP = original.memberPP;
        this.memberOP = original.memberOP;
        this.contRateOPEr = original.contRateOPEr;
        this.contRateOPEe = original.contRateOPEe;
        this.contRatePP = original.contRatePP;
        this.wealth = original.wealth;
    }


    /**************************************************************
     * DEDICATED METHODS
     **************************************************************/

    public void membership(Person person, boolean memberOPL1, boolean memberPPL1, double innov1, double innov2) {

        if (Les_c4.EmployedOrSelfEmployed.equals(person.getLes_c4())) {

            memberOP = ManagerRegressions.getAnnualEventFromBiennial(person, memberOPL1, innov1, RegressionName.WealthPensionPW1a, RegressionName.WealthPensionPW1b);
            memberPP = ManagerRegressions.getAnnualEventFromBiennial(person, memberPPL1, innov2, RegressionName.WealthPensionPW2a, RegressionName.WealthPensionPW2b);
        } else {

            memberOP = false;
            memberPP = false;
        }
    }

    public void contributionRates(Person person, double innov1, double innov2, double innov3, double innov4, double innov5) {

        if (memberOP) {

            // consider employee contributions first
            PensionContribRate pcrDiscrete = ManagerRegressions.getEvent(person, RegressionName.WealthPensionPW1c, innov1);
            if (pcrDiscrete.equals(PensionContribRate.Other)) {

                double score, rmse, gauss, val;
                score = Parameters.getRegPW1d().getScore(person, Person.DoublesVariables.class);
                rmse = Parameters.getRMSEForRegression("PW1d");
                gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(innov2);
                val = Math.sinh(score + gauss * rmse);
                int test = (int)Math.round(val);
                while (test == 0 || test == 3 || test == 5) {

                    // need to re-draw
                    if (innov2 > 0.5) {
                        innov2 = (innov2 - 0.5) / 0.5;
                    } else {
                        innov2 = innov2 / 0.5;
                    }
                    gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(innov2);
                    val = Math.sinh(score + gauss * rmse);
                    test = (int)Math.round(val);
                }
                contRateOPEe = val / 100.0;
            } else {
                contRateOPEe = pcrDiscrete.getValue() / 100.0;
            }

            // consider employer contributions
            pcrDiscrete = ManagerRegressions.getEvent(person, RegressionName.WealthPensionPW1e, innov3);
            if (pcrDiscrete.equals(PensionContribRate.Other)) {

                double score, rmse, gauss, val;
                score = Parameters.getRegPW1f().getScore(person, Person.DoublesVariables.class);
                rmse = Parameters.getRMSEForRegression("PW1f");
                gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(innov4);
                val = Math.sinh(score + gauss * rmse);
                int test = (int)Math.round(val);
                while (test == 0 || test == 3 || test == 5) {

                    // need to re-draw
                    if (innov2 > 0.5) {
                        innov2 = (innov2 - 0.5) / 0.5;
                    } else {
                        innov2 = innov2 / 0.5;
                    }
                    gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(innov4);
                    val = Math.sinh(score + gauss * rmse);
                    test = (int)Math.round(val);
                }
                contRateOPEr = val / 100.0;
            } else {
                contRateOPEr = pcrDiscrete.getValue() / 100.0;
            }
        } else {

            contRateOPEe = 0.0;
            contRateOPEr = 0.0;
        }
        if (memberPP) {

            double score, rmse, gauss;
            score = Parameters.getRegPW2c().getScore(person, Person.DoublesVariables.class);
            rmse = Parameters.getRMSEForRegression("PW2c");
            gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(innov5);
            contRatePP = Math.exp(score + gauss * rmse);
        }
    }

    public void projectWealth(double wealthL1, double employmentIncomeAnnual, double growthRate, Les_c4 labC4) {

        wealth = wealthL1 * (1.0 + growthRate);
        if (!Les_c4.EmployedOrSelfEmployed.equals(labC4)) {

            contRateOPEe = 0.0;
            contRateOPEr = 0.0;
            contRatePP = 0.0;
        } else {

            if (!memberOP) {
                contRateOPEe = 0.0;
                contRateOPEr = 0.0;
            }
            if (!memberPP) {
                contRatePP = 0.0;
            }
            wealth += (contRatePP + contRateOPEe + contRateOPEr) * employmentIncomeAnnual;
        }
    }


    /**************************************************************
     * GETTERS AND SETTERS
     **************************************************************/

    public boolean isMemberPP() {
        return memberPP;
    }

    public void setMemberPP(boolean memberPP) {
        this.memberPP = memberPP;
    }

    public boolean isMemberOP() {
        return memberOP;
    }

    public void setMemberOP(boolean memberOP) {
        this.memberOP = memberOP;
    }

    public double getContRateOPEr() {
        return contRateOPEr;
    }

    public void setContRateOPEr(double contRateOPEr) {
        this.contRateOPEr = contRateOPEr;
    }

    public double getContRateOPEe() {
        return contRateOPEe;
    }

    public void setContRateOPEe(double contRateOPEe) {
        this.contRateOPEe = contRateOPEe;
    }

    public double getContRatePP() {
        return contRatePP;
    }

    public void setContRatePP(double contRatePP) {
        this.contRatePP = contRatePP;
    }

    public double getWealth() {
        return wealth;
    }

    public void setWealth(double wealth) {
        this.wealth = wealth;
    }

}
