/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.channel;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.FeeType;
import com.divudi.ejb.ChannelBean;
import com.divudi.entity.Bill;
import com.divudi.entity.BillSession;
import com.divudi.entity.BilledBill;
import com.divudi.entity.ItemFee;
import com.divudi.entity.Patient;
import com.divudi.entity.Person;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.BillSessionFacade;
import com.divudi.facade.InstitutionFacade;
import com.divudi.facade.ItemFeeFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.ServiceSessionFacade;
import com.divudi.facade.StaffFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class BookingController implements Serializable {

    private Speciality speciality;
    private Staff staff;
    private ServiceSession selectedServiceSession;
    private BillSession selectedBillSession;
    ////////////////////
    private List<ServiceSession> serviceSessions;
    private List<BillSession> billSessions;
    ////////////////////
    @Inject
    private SessionController sessionController;
    @Inject
    private ChannelBillController channelCancelController;
    @Inject
    private ChannelReportController channelReportController;
    @Inject
    private ChannelSearchController channelSearchController;
    ///////////////////
    @EJB
    private StaffFacade staffFacade;
    @EJB
    private ServiceSessionFacade serviceSessionFacade;
    @EJB
    private BillSessionFacade billSessionFacade;
    @EJB
    private InstitutionFacade institutionFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PersonFacade personFacade;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    ItemFeeFacade ItemFeeFacade;
    /////////////////////////
    @EJB
    private ChannelBean channelBean;

    public String nurse() {
        if (preSet()) {
            return "channel_nurse_view";
        } else {
            return "";
        }
    }

    public String doctor() {
        if (preSet()) {
            return "channel_doctor_view";
        } else {
            return "";
        }
    }

    public String session() {
        if (preSet()) {
            return "channel_session_view";
        } else {
            return "";
        }
    }

    public String phone() {
        if (preSet()) {
            return "channel_phone_view";
        } else {
            return "";
        }
    }

    public String user() {
        if (preSet()) {
            return "channel_user_view";
        } else {
            return "";
        }
    }

    public void updatePatient() {
        getBillSessionFacade().edit(getSelectedBillSession());

        getPatientFacade().edit(getSelectedBillSession().getBill().getPatient());
        UtilityController.addSuccessMessage("Patient Updated");
    }

    public void makeNull() {
        speciality = null;
        staff = null;
        selectedServiceSession = null;
        /////////////////////
        serviceSessions = null;
        billSessions = null;
    }

    public List<Staff> completeStaff(String query) {
        List<Staff> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            if (getSpeciality() != null) {
                sql = "select p from Staff p where p.retired=false and (upper(p.person.name) like '%" + query.toUpperCase() + "%'or  upper(p.code) like '%" + query.toUpperCase() + "%' ) and p.speciality.id = " + getSpeciality().getId() + " order by p.person.name";
            } else {
                sql = "select p from Staff p where p.retired=false and (upper(p.person.name) like '%" + query.toUpperCase() + "%'or  upper(p.code) like '%" + query.toUpperCase() + "%' ) order by p.person.name";
            }
            //System.out.println(sql);
            suggestions = getStaffFacade().findBySQL(sql);
        }
        return suggestions;
    }

    public List<Staff> getConsultants() {
        List<Staff> suggestions;
        String sql;

        if (getSpeciality() != null) {
            sql = "select p from Staff p where p.retired=false and p.speciality.id = " + getSpeciality().getId() + " order by p.person.name";
        } else {
            sql = "select p from Doctor p where p.retired=false order by p.person.name";
        }
        //System.out.println(sql);
        suggestions = getStaffFacade().findBySQL(sql);

        return suggestions;
    }

    /**
     * Creates a new instance of BookingController
     */
    public BookingController() {
    }

    public Speciality getSpeciality() {
        return speciality;
    }

    public void setSpeciality(Speciality speciality) {
        makeNull();
        this.speciality = speciality;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public void setStaffFacade(StaffFacade staffFacade) {
        this.staffFacade = staffFacade;
    }

    public void updateChargesForServiceSession(List<ServiceSession> lstSs) {
        for(ServiceSession ss:lstSs){
            updateChargesForServiceSession(ss);
        }
    }
    
    public void updateChargesForServiceSession(ServiceSession ss) {
        List<ItemFee> fs;
        String jpql;
        Map m = new HashMap();
        jpql = "Select f from ItemFee f "
                + " where f.retired=false and "
                + " f.item=:ses "
                + " order by f.id";
        m.put("ses", ss);
        fs = getItemFeeFacade().findBySQL(jpql, m);

        ss.setHospitalFee(0.0);
        ss.setProfessionalFee(0.0);
        ss.setTaxFee(0.0);
        ss.setOtherFee(0.0);
        ss.setTotalFee(0.0);

        ss.setHospitalFfee(0.0);
        ss.setProfessionalFfee(0.0);
        ss.setTaxFfee(0.0);
        ss.setOtherFfee(0.0);
        ss.setTotalFfee(0.0);

        ss.setItemFees(new ArrayList<ItemFee>());
        
        for (ItemFee f : fs) {
            if (f.getFeeType() == FeeType.OwnInstitution) {
                ss.setHospitalFee(ss.getHospitalFee() + f.getFee());
                ss.setHospitalFfee(ss.getHospitalFfee() + f.getFfee());
            }else if(f.getFeeType() == FeeType.Staff) {
                ss.setProfessionalFee(ss.getProfessionalFee() + f.getFee());
                ss.setProfessionalFfee(ss.getProfessionalFfee() + f.getFfee());
            }else  if(f.getFeeType() == FeeType.Tax) {
                ss.setTaxFee(ss.getProfessionalFee() + f.getFee());
                ss.setTaxFfee(ss.getProfessionalFfee() + f.getFfee());
            }else{
                ss.setOtherFee(ss.getOtherFee() + f.getFee());
                ss.setOtherFfee(ss.getOtherFfee() + f.getFfee());
            }
            ss.getItemFees().add(f);
        }
        ss.setTotalFee(ss.getHospitalFee() + ss.getProfessionalFee() + ss.getTaxFee() + ss.getOtherFee());
        ss.setTotalFfee(ss.getHospitalFfee() + ss.getProfessionalFfee() + ss.getTaxFfee() + ss.getOtherFfee());
    }

    public void generateSessions() {
        serviceSessions = new ArrayList<>();
        String sql;
        Map m = new HashMap();
        m.put("staff", getStaff());
        if (staff != null) {
            sql = "Select s From ServiceSession s where s.retired=false and s.staff=:staff order by s.sessionWeekday";
            List<ServiceSession> tmp = getServiceSessionFacade().findBySQL(sql, m);
            updateChargesForServiceSession(tmp);
            serviceSessions = getChannelBean().generateDailyServiceSessionsFromWeekdaySessions(tmp);
        }
    }

    public List<ServiceSession> getServiceSessions() {
        return serviceSessions;
    }

    public void setServiceSessions(List<ServiceSession> serviceSessions) {

        this.serviceSessions = serviceSessions;
    }

    public ServiceSessionFacade getServiceSessionFacade() {
        return serviceSessionFacade;
    }

    public void setServiceSessionFacade(ServiceSessionFacade serviceSessionFacade) {
        this.serviceSessionFacade = serviceSessionFacade;
    }

    public List<BillSession> getBillSessions() {

        if (getSelectedServiceSession() != null) {
            String sql = "Select bs From BillSession bs where bs.retired=false and bs.serviceSession.id=" + getSelectedServiceSession().getId() + " and bs.sessionDate= :ssDate";
            HashMap hh = new HashMap();
            hh.put("ssDate", getSelectedServiceSession().getSessionAt());
            billSessions = getBillSessionFacade().findBySQL(sql, hh, TemporalType.DATE);

        }

        return billSessions;
    }

    public void setBillSessions(List<BillSession> billSessions) {
        this.billSessions = billSessions;
    }

    public ServiceSession getSelectedServiceSession() {
        return selectedServiceSession;
    }

    public void setSelectedServiceSession(ServiceSession selectedServiceSession) {
        this.selectedServiceSession = selectedServiceSession;
    }

    public void makeBillSessionNull() {
        billSessions = null;
    }

    public BillSessionFacade getBillSessionFacade() {
        return billSessionFacade;
    }

    public void setBillSessionFacade(BillSessionFacade billSessionFacade) {
        this.billSessionFacade = billSessionFacade;
    }

    public InstitutionFacade getInstitutionFacade() {
        return institutionFacade;
    }

    public void setInstitutionFacade(InstitutionFacade institutionFacade) {
        this.institutionFacade = institutionFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public PersonFacade getPersonFacade() {
        return personFacade;
    }

    public void setPersonFacade(PersonFacade personFacade) {
        this.personFacade = personFacade;
    }

    public PatientFacade getPatientFacade() {
        return patientFacade;
    }

    public void setPatientFacade(PatientFacade patientFacade) {
        this.patientFacade = patientFacade;
    }

    public BillSession getSelectedBillSession() {
        if (selectedBillSession == null) {
            selectedBillSession = new BillSession();
            Bill b = new BilledBill();
            Patient p = new Patient();
            p.setPerson(new Person());
            b.setPatient(p);
            selectedBillSession.setBill(b);
        }
        return selectedBillSession;
    }

    public void setSelectedBillSession(BillSession selectedBillSession) {
        this.selectedBillSession = selectedBillSession;
        getChannelCancelController().makeNull();
        getChannelCancelController().setBillSession(selectedBillSession);
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public ChannelBillController getChannelCancelController() {
        return channelCancelController;
    }

    public void setChannelCancelController(ChannelBillController channelCancelController) {
        this.channelCancelController = channelCancelController;
    }

    public ChannelReportController getChannelReportController() {
        return channelReportController;
    }

    public void setChannelReportController(ChannelReportController channelReportController) {
        this.channelReportController = channelReportController;
    }

    public Boolean preSet() {
        if (getSelectedServiceSession() == null) {
            UtilityController.addErrorMessage("Please select Service Session");
            return false;
        }

        getChannelReportController().setServiceSession(selectedServiceSession);
        return true;
    }

    public ChannelSearchController getChannelSearchController() {
        return channelSearchController;
    }

    public void setChannelSearchController(ChannelSearchController channelSearchController) {
        this.channelSearchController = channelSearchController;
    }

    public ChannelBean getChannelBean() {
        return channelBean;
    }

    public void setChannelBean(ChannelBean channelBean) {
        this.channelBean = channelBean;
    }

    public ItemFeeFacade getItemFeeFacade() {
        return ItemFeeFacade;
    }

    public void setItemFeeFacade(ItemFeeFacade ItemFeeFacade) {
        this.ItemFeeFacade = ItemFeeFacade;
    }

}
