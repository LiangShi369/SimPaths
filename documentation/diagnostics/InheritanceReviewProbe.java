package simpaths.model;

import java.util.List;
import java.util.Set;
import simpaths.model.benefitunit.WealthNonPension;
import simpaths.model.enums.SampleExit;
import simpaths.model.person.PrivatePension;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyDouble;

public class InheritanceReviewProbe {
    private static Person person(long id, int age) {
        Person p = mock(Person.class);
        when(p.getId()).thenReturn(id);
        when(p.getDemAge()).thenReturn(age);
        when(p.getSampleExit()).thenReturn(SampleExit.NotYet);
        when(p.getPrivatePension()).thenReturn(new PrivatePension());
        return p;
    }
    private static WealthNonPension wire(BenefitUnit unit, double balance, Person... people) {
        WealthNonPension w = new WealthNonPension();
        w.setWealthFinancialValue(balance);
        when(unit.getMembers()).thenReturn(Set.of(people));
        when(unit.getWealthNonPension()).thenAnswer(i -> new WealthNonPension(w));
        for (Person p : people) when(p.getBenefitUnit()).thenReturn(unit);
        doAnswer(i -> {w.setWealthFinancialValue(w.getWealthFinancial().getValue()+
                i.getArgument(0,Double.class));return null;}).when(unit).addInheritanceCash(anyDouble());
        doAnswer(i -> {w.setWealthFinancialValue(0);return null;}).when(unit).clearEstateWealth();
        return w;
    }
    public static void main(String[] args) {
        Person mother=person(1,75),father=person(2,74),child=person(3,45),standIn=person(4,50);
        when(mother.getInheritanceSourceId()).thenReturn(101L);
        when(father.getInheritanceSourceId()).thenReturn(102L);
        when(child.getInheritanceSourceId()).thenReturn(103L);
        when(child.getIdMother()).thenReturn(1L);
        when(mother.getSampleExit()).thenReturn(SampleExit.Death);
        when(father.getSampleExit()).thenReturn(SampleExit.DeathAlignment);
        BenefitUnit home=mock(BenefitUnit.class),childUnit=mock(BenefitUnit.class),standInUnit=mock(BenefitUnit.class);
        wire(home,100000,mother,father);
        WealthNonPension childWealth=wire(childUnit,0,child),standInWealth=wire(standInUnit,0,standIn);
        when(home.getMale()).thenReturn(father);when(home.getFemale()).thenReturn(mother);
        when(childUnit.getMale()).thenReturn(child);when(standInUnit.getMale()).thenReturn(standIn);
        Person sourceChild=person(103,45);
        when(sourceChild.getIdMother()).thenReturn(101L);when(sourceChild.getIdFather()).thenReturn(102L);
        BenefitUnit sourceUnit=mock(BenefitUnit.class);when(sourceUnit.getMembers()).thenReturn(Set.of(sourceChild));
        Household sourceHousehold=mock(Household.class);when(sourceHousehold.getBenefitUnits()).thenReturn(Set.of(sourceUnit));
        InheritanceModule module=new InheritanceModule();module.indexSourceFamilies(List.of(sourceHousehold));
        module.settleYear(2030,Set.of(mother,father,child,standIn),Set.of(home,childUnit,standInUnit));
        System.out.println("SHARED_CHILD linked_child="+childWealth.getWealthFinancial().getValue()+
                " synthetic_duplicate_branch="+standInWealth.getWealthFinancial().getValue());
        if(childWealth.getWealthFinancial().getValue()!=100000 || standInWealth.getWealthFinancial().getValue()!=0)
            throw new AssertionError("Shared-child deduplication must pay the identified child the full estate");

        Person adult=person(10,80),otherAdult=person(11,78),minor=person(20,17),recipient=person(30,55);
        when(adult.getSampleExit()).thenReturn(SampleExit.Death);
        when(otherAdult.getSampleExit()).thenReturn(SampleExit.Death);
        when(minor.getSampleExit()).thenReturn(SampleExit.DeathAlignment);
        when(minor.getIdFather()).thenReturn(10L);
        when(minor.getIdMother()).thenReturn(11L);
        BenefitUnit estate=mock(BenefitUnit.class),recipientUnit=mock(BenefitUnit.class);
        wire(estate,100000,adult,otherAdult,minor);wire(recipientUnit,0,recipient);
        when(estate.getMale()).thenReturn(adult);when(estate.getFemale()).thenReturn(otherAdult);
        when(recipientUnit.getMale()).thenReturn(recipient);
        module=new InheritanceModule();
        module.settleYear(2030,Set.of(adult,otherAdult,minor,recipient),Set.of(estate,recipientUnit));
        System.out.println("REPRESENTATIVE_DONOR "+module.getTransfers().getFirst().deceasedId()+
                " age="+otherAdult.getDemAge()+" excluded_dependent="+minor.getId());
        if(module.getTransfers().getFirst().deceasedId()!=11)
            throw new AssertionError("Expected highest-ID deceased responsible adult, not dependent");
    }
}
