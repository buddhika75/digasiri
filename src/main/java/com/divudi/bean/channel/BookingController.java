/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.channel;

import com.divudi.bean.common.DoctorSpecialityController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.SmsController;
import com.divudi.bean.common.UtilityController;
import com.divudi.bean.report.ChannelSummeryRow;
import com.divudi.data.ApplicationInstitution;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.PersonInstitutionType;
import com.divudi.data.SmsType;
import com.divudi.data.channel.ChannelScheduleEvent;
import com.divudi.ejb.ChannelBean;
import com.divudi.ejb.CommonFunctions;
import com.divudi.ejb.FinalVariables;
import com.divudi.entity.AgentHistory;
import com.divudi.entity.Bill;
import com.divudi.entity.BillComponent;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillFeePayment;
import com.divudi.entity.BillItem;
import com.divudi.entity.BillSession;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Item;
import com.divudi.entity.ItemFee;
import com.divudi.entity.Patient;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.Payment;
import com.divudi.entity.Person;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.entity.channel.ArrivalRecord;
import com.divudi.facade.AgentHistoryFacade;
import com.divudi.facade.BillComponentFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.BillSessionFacade;
import com.divudi.facade.FingerPrintRecordFacade;
import com.divudi.facade.InstitutionFacade;
import com.divudi.facade.ItemFeeFacade;
import com.divudi.facade.PatientEncounterFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PaymentFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.ServiceSessionFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.facade.util.JsfUtil;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.DefaultScheduleModel;
import org.primefaces.model.ScheduleModel;
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class BookingController implements Serializable {

    /**
     * EJBs
     */
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
    @EJB
    private ChannelBean channelBean;
    @EJB
    FingerPrintRecordFacade fpFacade;
    @EJB
    private FinalVariables finalVariables;
    @EJB
    private CommonFunctions commonFunctions;
    /**
     * Controllers
     */
    @Inject
    private SessionController sessionController;
    @Inject
    private ChannelBillController channelCancelController;
    @Inject
    private ChannelReportController channelReportController;
    @Inject
    private ChannelSearchController channelSearchController;
    @Inject
    ServiceSessionLeaveController serviceSessionLeaveController;
    @Inject
    ChannelBillController channelBillController;
    @Inject
    DoctorSpecialityController doctorSpecialityController;
    @Inject
    ChannelStaffPaymentBillController channelStaffPaymentBillController;
    @Inject
    private SmsController smsController;

    /**
     * Properties
     */
    private Speciality speciality;
    private Staff staff;
    double absentPercentage;

    @Temporal(javax.persistence.TemporalType.DATE)
    Date channelDay;
    private ServiceSession selectedServiceSession;
    private BillSession selectedBillSession;
    private List<ServiceSession> serviceSessions;
    private List<BillSession> billSessions;
    List<Staff> consultants;
    List<BillSession> getSelectedBillSession;
    boolean printPreview;
    double absentCount;
    int serealNo;
    Date date;
    Date sessionStartingDate;
    String selectTextSpeciality = "";
    String selectTextConsultant = "";
    String selectTextSession = "";
    ArrivalRecord arrivalRecord;
    PaymentMethod canPayMetTmp;

    Date fromDate;
    Date toDate;

    private double staffFaee;
    private double hospitalFee;
    private double scanFee;
    private double vat;
    private double totalFee;

    private ScheduleModel eventModel;

    private ChannelScheduleEvent event = new ChannelScheduleEvent();
    private List<ChannelSummeryRow> channelSummeryRows;

    public String fillAllDoctorBookingSummery() {

        channelSummeryRows = new ArrayList<>();

        String sql;
        Map m = new HashMap();
        m.put("fromDate", fromDate);
        m.put("toDate", fromDate);
        m.put("paidAmount", 0.0);
        List<BillType> bts = new ArrayList<>();
        bts.add(BillType.ChannelAgent);
        bts.add(BillType.ChannelCash);
        bts.add(BillType.ChannelOnCall);
        bts.add(BillType.ChannelStaff);
        m.put("tbs", bts);

        sql = "select bs.serviceSession.staff, count(bs), sum(bs.bill.staffFee), sum(bs.bill.hospitalFee), sum(bs.bill.netTotal), sum(bs.bill.vat) ";
        sql += "from BillSession bs "
                + " where bs.retired=false and "
                + " bs.bill.paidAmount > :paidAmount and "
                + " bs.bill.billType in :tbs and "
                + " bs.sessionDate between :fromDate and :toDate ";

        List<Object[]> allBills = billFacade.findObjectsArrayBySQL(sql, m, TemporalType.TIMESTAMP);

        sql = "select bs.serviceSession.staff, count(bs), sum(bs.bill.staffFee), sum(bs.bill.hospitalFee), sum(bs.bill.netTotal), sum(bs.bill.vat) ";
        sql += "from BillSession bs "
                + " where bs.retired=false and "
                + " bs.bill.cancelled=true and "
                + " bs.bill.paidAmount > :paidAmount and "
                + " bs.bill.refunded=false and "
                + " bs.bill.billType in :tbs and "
                + " bs.sessionDate between :fromDate and :toDate ";
        List<Object[]> allCancelBills = billFacade.findObjectsArrayBySQL(sql, m, TemporalType.TIMESTAMP);

        sql = "select bs.serviceSession.staff, count(bs), sum(bs.bill.staffFee), sum(bs.bill.hospitalFee), sum(bs.bill.netTotal), sum(bs.bill.vat) ";
        sql += "from BillSession bs "
                + " where bs.retired=false and "
                + " bs.bill.cancelled=false and "
                + " bs.bill.paidAmount > :paidAmount and "
                + " bs.bill.refunded=true and "
                + " bs.bill.billType in :tbs and "
                + " bs.sessionDate between :fromDate and :toDate ";
        List<Object[]> allRefundBills = billFacade.findObjectsArrayBySQL(sql, m, TemporalType.TIMESTAMP);

        if (false) {
            BillSession s = new BillSession();
            s.getServiceSession().getStaff();
//            bs.bill.staffFee, bs.bill.hospitalFee, bs.bill.netTotal, bs.bill.vat,
        }

        if (!allBills.isEmpty()) {
            for (Object[] ob : allBills) {
                ChannelSummeryRow row = new ChannelSummeryRow();

                Staff doc = null;
                if (ob[0] instanceof Staff) {
                    doc = (Staff) ob[0];
                } else {
                    continue;
                }

                long count = (long) ob[1];
                if (count == 0) {
                    continue;
                }

                Double staffFee = (Double) ob[2];
                Double hosFee = (Double) ob[3];
                Double totFee = (Double) ob[4];
                Double vatFee = (Double) ob[5];

                row.setStaff(doc);
                row.setCountBilled(count);
                row.setStaffFeeBilled(staffFee);
                row.setHospitalFeeBilled(hosFee);
                row.setTotalFeeBilled(totFee);
                row.setVatBilled(vatFee);
                channelSummeryRows.add(row);

            }

        }

        if (!allCancelBills.isEmpty()) {
            for (Object[] ob : allCancelBills) {

                Staff doc = null;
                if (ob[0] instanceof Staff) {
                    doc = (Staff) ob[0];
                } else {
                    continue;
                }

                ChannelSummeryRow row;
                boolean found = false;
                for (ChannelSummeryRow csr : channelSummeryRows) {
                    if (csr.getStaff().equals(doc)) {
                        found = true;
                        row = csr;
                    }
                }

                if (!found) {
                    row = new ChannelSummeryRow();
                    channelSummeryRows.add(row);
                } else {
                    continue;
                }

                long count = (long) ob[1];
                if (count == 0) {
                    continue;
                }

                Double staffFee = (Double) ob[2];
                Double hosFee = (Double) ob[3];
                Double totFee = (Double) ob[4];
                Double vatFee = (Double) ob[5];

                row.setCountCancelled(count);
                row.setStaffFeeCancelled(staffFee);
                row.setHospitalFeeCancelled(hosFee);
                row.setTotalFeeCancelled(totFee);
                row.setVatCancelled(vatFee);

            }

        }

        if (!allRefundBills.isEmpty()) {
            for (Object[] ob : allRefundBills) {

                Staff doc = null;
                if (ob[0] instanceof Staff) {
                    doc = (Staff) ob[0];
                } else {
                    continue;
                }

                ChannelSummeryRow row;
                boolean found = false;
                for (ChannelSummeryRow csr : channelSummeryRows) {
                    if (csr.getStaff().equals(doc)) {
                        found = true;
                        row = csr;
                    }
                }

                if (!found) {
                    row = new ChannelSummeryRow();
                    channelSummeryRows.add(row);
                } else {
                    continue;
                }

                long count = (long) ob[1];
                if (count == 0) {
                    continue;
                }

                Double staffFee = (Double) ob[2];
                Double hosFee = (Double) ob[3];
                Double totFee = (Double) ob[4];
                Double vatFee = (Double) ob[5];

                row.setCountRefunded(count);
                row.setStaffFeeRefunded(staffFee);
                row.setHospitalFeeRefunded(hosFee);
                row.setTotalFeeRefunded(totFee);
                row.setVatRefunded(vatFee);

            }

        }

        hospitalFee = 0.0;
        staffFaee = 0.0;
        totalFee = 0.0;
        vat = 0.0;

        for (ChannelSummeryRow row : channelSummeryRows) {
            row.setCount(row.getCountBilled() - row.getCountCancelled() - row.getCountRefunded());
            row.setStaffFee(row.getStaffFeeBilled() - row.getStaffFeeCancelled() - row.getStaffFeeRefunded());
            row.setHospitalFee(row.getHospitalFeeBilled() - row.getHospitalFeeCancelled());
            row.setTotalFee(row.getTotalFeeBilled() - row.getTotalFeeCancelled() - (row.getTotalFeeRefunded() - row.getHospitalFeeRefunded()));
            row.setVat(row.getVatBilled() - row.getVatCancelled() - row.getVatRefunded());
            hospitalFee += row.getHospitalFee();
            staffFaee += row.getStaffFee();
            totalFee += row.getTotalFee();
            vat += row.getVat();
        }

        return "/channel/all_doctor_booking_summery";

    }

    public String fillSingleDoctorBookings() {
        String sql = "Select bs From BillSession bs "
                + " where bs.retired=false and "
                + " bs.absent!=true and "
                + " bs.bill.cancelled=false and "
                + " bs.bill.paidAmount > :paidAmount and "
                + " bs.bill.refunded=false and "
                + " bs.bill.billType in :tbs and "
                + " bs.serviceSession.staff=:staff and bs.sessionDate between :fromDate and :toDate "
                + " order by bs.serialNo";
        HashMap hh = new HashMap();
        hh.put("staff", staff);
        hh.put("fromDate", fromDate);
        hh.put("toDate", fromDate);
        List<BillType> bts = new ArrayList<>();
        bts.add(BillType.ChannelAgent);
        bts.add(BillType.ChannelCash);
        bts.add(BillType.ChannelOnCall);
        bts.add(BillType.ChannelStaff);
        hh.put("tbs", bts);
        hh.put("paidAmount", 0.0);
        billSessions = getBillSessionFacade().findBySQL(sql, hh, TemporalType.DATE);
        totalFee = 0.0;
        hospitalFee = 0.0;
        scanFee = 0.0;
        vat = 0.0;
        staffFaee = 0.0;
        for (BillSession cbs : billSessions) {
            staffFaee += cbs.getBill().getStaffFee();
            vat += cbs.getBill().getVat();
            hospitalFee += cbs.getBill().getHospitalFee();
            totalFee += cbs.getBill().getNetTotal();
        }

        return "/channel/single_doctor_bookings";
    }

    public String fillAllDoctorBookings() {
        String sql = "Select bs From BillSession bs "
                + " where bs.retired=false and "
                + " bs.absent!=true and "
                + " bs.bill.cancelled=false and "
                + " bs.bill.paidAmount > :paidAmount and "
                + " bs.bill.refunded=false and "
                + " bs.bill.billType in :tbs and "
                + " bs.sessionDate between :fromDate and :toDate "
                + " order by bs.serialNo";
        HashMap hh = new HashMap();
        hh.put("fromDate", fromDate);
        hh.put("toDate", fromDate);
        List<BillType> bts = new ArrayList<>();
        bts.add(BillType.ChannelAgent);
        bts.add(BillType.ChannelCash);
        bts.add(BillType.ChannelOnCall);
        bts.add(BillType.ChannelStaff);
        hh.put("tbs", bts);
        hh.put("paidAmount", 0.0);
        billSessions = getBillSessionFacade().findBySQL(sql, hh, TemporalType.DATE);
        totalFee = 0.0;
        hospitalFee = 0.0;
        scanFee = 0.0;
        vat = 0.0;
        staffFaee = 0.0;
        for (BillSession cbs : billSessions) {
            staffFaee += cbs.getBill().getStaffFee();
            vat += cbs.getBill().getVat();
            hospitalFee += cbs.getBill().getHospitalFee();
            totalFee += cbs.getBill().getNetTotal();
        }

        return "/channel/all_doctor_bookings";
    }

    public String nurse() {
        if (preSet()) {
            getChannelReportController().fillNurseView();
            return "channel_nurse_view";
        } else {
            return "";
        }
    }

    public String doctor() {
        if (preSet()) {
            getChannelReportController().fillDoctorView();
            return "channel_doctor_view";
        } else {
            return "";
        }
    }

    public String paidView() {
        if (preSet()) {
            getChannelReportController().fillPaidView();
            return "channel_paid_view";
        } else {
            return "";
        }
    }

    public String paidAllView() {
        if (preSet()) {
            getChannelReportController().fillPaidAllView();
            return "channel_paid_all_view";
        } else {
            return "";
        }
    }

    public String presentView() {
        if (preSet()) {
            getChannelReportController().fillPresentView();
            return "channel_present_view";
        } else {
            return "";
        }
    }

    public String presentAllView() {
        if (preSet()) {
            getChannelReportController().fillPresentAllView();
            return "channel_present_all_view";
        } else {
            return "";
        }
    }

    public String presentAllViewPeriod() {
        getChannelReportController().fillPresentAllViewForPeriod(fromDate, toDate);
        return "channel_present_all_period_view";
    }

    public String session() {
        if (preSet()) {
            getChannelReportController().fillSessions();
            return "channel_session_view";
        } else {
            return "";
        }
    }

    public String phone() {
        if (preSet()) {
            getChannelReportController().fillNurseViewPb();
            return "channel_phone_view";
        } else {
            return "";
        }
    }

    public String user() {
        if (preSet()) {
            getChannelReportController().fillNurseView();
            return "channel_user_view";
        } else {
            return "";
        }
    }

    public List<BillSession> getGetSelectedBillSession() {
        return getSelectedBillSession;
    }

    public void setGetSelectedBillSession(List<BillSession> getSelectedBillSession) {
        this.getSelectedBillSession = getSelectedBillSession;
    }

    public boolean errorCheckForSerial() {
        boolean alreadyExists = false;
        for (BillSession bs : billSessions) {
            //System.out.println("billSessions" + bs.getId());

            if (selectedBillSession.equals(bs)) {

            } else {
                if (bs.getSerialNo() == selectedBillSession.getSerialNo()) {
                    alreadyExists = true;
                }
            }
            System.out.println("bs = " + bs);
            if (!bs.equals(selectedBillSession)) {
                for (BillItem bi : bs.getBill().getBillItems()) {
                    System.out.println("bi.getBillSession().getSerialNo() = " + bi.getBillSession().getSerialNo());
                    if (serealNo == bi.getBillSession().getSerialNo()) {
                        alreadyExists = true;
                        UtilityController.addErrorMessage("This Number Is Alredy Exsist");
                    }
                }
            }

        }

        return alreadyExists;
    }

    public boolean errorCheck() {
        boolean flag = false;
        if (serealNo == 0) {
            UtilityController.addErrorMessage("Cant Add This Number");
            return true;
        }
        for (BillSession bs : billSessions) {
            System.out.println("bs = " + bs);
            if (!bs.equals(selectedBillSession)) {
                for (BillItem bi : bs.getBill().getBillItems()) {
                    System.out.println("bi.getBillSession().getSerialNo() = " + bi.getBillSession().getSerialNo());
                    if (serealNo == bi.getBillSession().getSerialNo()) {
                        UtilityController.addErrorMessage("This Number Is Alredy Exsist");
                        flag = true;
                    }
                }
            }

        }

        return flag;
    }

    public double getAbsentCount() {
        return absentCount;
    }

    public void setAbsentCount(double absentCount) {
        this.absentCount = absentCount;
    }

//    public void errorCheckChannelNumber() {
//
//        for (BillSession bs : billSessions) {
//            //System.out.println("billSessions" + bs.getName());
//            for (BillItem bi : getSelectedBillSession().getBill().getBillItems()) {
//                //System.out.println("billitem" + bi.getId());
//                if (bs.getSerialNo() == bi.getBillSession().getSerialNo()) {
//                    UtilityController.addErrorMessage("Number you entered already exist");
//                    setSelectedBillSession(bs);
//
//                }
//
//            }
//        }
//
//    }
    public void updatePatient() {
        getPersonFacade().edit(getSelectedBillSession().getBill().getPatient().getPerson());
        UtilityController.addSuccessMessage("Patient Updated");
    }

    public void updateSerial() {
        if (errorCheckForSerial()) {
            return;
        }
        if (errorCheck()) {
            return;
        }

        for (BillItem bi : getSelectedBillSession().getBill().getBillItems()) {
            bi.getBillSession().setSerialNo(serealNo);
            System.out.println("bi = " + bi.getBillSession().getSerialNo());
            getBillItemFacade().edit(bi);
        }

        getBillSessionFacade().edit(getSelectedBillSession());
        //System.out.println(getSelectedBillSession().getBill().getPatient());
        UtilityController.addSuccessMessage("Serial Updated");
    }

    public void listnerMarkAbsent() {
        if (getSelectedBillSession().isAbsent()) {
            getSelectedBillSession().setAbsentMarkedAt(new Date());
            getSelectedBillSession().setAbsentMarkedUser(getSessionController().getLoggedUser());
        } else {
            getSelectedBillSession().setAbsentUnmarkedAt(new Date());
            getSelectedBillSession().setAbsentUnmarkedUser(getSessionController().getLoggedUser());
        }

        getBillSessionFacade().edit(getSelectedBillSession());
        //System.out.println(getSelectedBillSession().getBill().getPatient());
        if (getSelectedBillSession().isAbsent()) {
            UtilityController.addSuccessMessage("Mark As Absent");
            if (getSelectedBillSession().getBill().getPaidBill() != null) {
                getSelectedBillSession().getBill().getPaidBill().getSingleBillSession().setAbsent(true);
                getBillSessionFacade().edit(getSelectedBillSession().getBill().getPaidBill().getSingleBillSession());
            }
        } else {
            UtilityController.addSuccessMessage("Mark As Present");
            if (getSelectedBillSession().getBill().getPaidBill() != null) {
                getSelectedBillSession().getBill().getPaidBill().getSingleBillSession().setAbsent(false);
                getBillSessionFacade().edit(getSelectedBillSession().getBill().getPaidBill().getSingleBillSession());
            }
        }
    }

    public void markBulkAsAbsent() {
        List<BillSession> bss = new ArrayList<>();
        String sql = "Select bs From BillSession bs "
                + " where bs.retired=false and "
                + " bs.bill.cancelled=false and "
                + " bs.bill.paidAmount > :paidAmount and "
                + " bs.bill.refunded=false and "
                + " bs.bill.billType in :tbs and "
                + " bs.sessionDate between :ssFrom and :ssTo ";
        sql += " order by bs.serialNo";
        HashMap hh = new HashMap();
        hh.put("ssFrom", getFromDate());
        hh.put("ssTo", getToDate());
        List<BillType> bts = new ArrayList<>();
        bts.add(BillType.ChannelAgent);
        bts.add(BillType.ChannelCash);
        bts.add(BillType.ChannelOnCall);
        bts.add(BillType.ChannelStaff);
        hh.put("tbs", bts);
        hh.put("paidAmount", 0.0);
        System.out.println("sql = " + sql);
        System.out.println("hh = " + hh);
        bss = getBillSessionFacade().findBySQL(sql, hh, TemporalType.DATE);
        System.out.println("bss.size = " + bss.size());
        Double countToMarkAbsent;
        if (absentPercentage > 100) {
            absentPercentage = 100;
        }
        countToMarkAbsent = (bss.size() / 100) * absentPercentage;
        System.out.println("countToMarkAbsent = " + countToMarkAbsent);
        int intCountToMarkAbsent = countToMarkAbsent.intValue();
        System.out.println("intCountToMarkAbsent = " + intCountToMarkAbsent);
        for (int i = 0; i < intCountToMarkAbsent; i++) {
            BillSession bs;
            do {
                int n = ThreadLocalRandom.current().nextInt(0, intCountToMarkAbsent);
                bs = bss.get(n);
            } while (!bs.isAbsent());
            bs.setAbsent(true);
            bs.setAbsentMarkedAt(new Date());
            bs.setAbsentMarkedUser(null);
            getBillSessionFacade().edit(bs);
        }
        UtilityController.addSuccessMessage("Marked Bulk as Absent");
    }

    Long di = 0l;

    public Long getDi() {
        return di;
    }

    public void setDi(Long di) {
        this.di = di;
    }

    public void deleteBulk() {
        System.out.println("delete bulk");
        di = 0l;
        List<BillSession> bss = new ArrayList<>();
        String sql = "Select bs From BillSession bs "
                + " where bs.retired=false and "
                + " bs.bill.cancelled=false and "
                + " bs.bill.paidAmount > :paidAmount and "
                + " bs.bill.refunded=false and "
                + " bs.bill.billType in :tbs and "
                + " bs.sessionDate between :ssFrom and :ssTo ";
        sql += " order by bs.serialNo";
        HashMap hh = new HashMap();
        hh.put("ssFrom", getFromDate());
        hh.put("ssTo", getToDate());
        List<BillType> bts = new ArrayList<>();
        bts.add(BillType.ChannelAgent);
        bts.add(BillType.ChannelCash);
        bts.add(BillType.ChannelOnCall);
        bts.add(BillType.ChannelStaff);
        hh.put("tbs", bts);
        hh.put("paidAmount", 0.0);
        bss = getBillSessionFacade().findBySQL(sql, hh, TemporalType.DATE);
        System.out.println("Absent Size = " + bss.size());
        Double countToMarkAbsent;
        if (absentPercentage > 100) {
            absentPercentage = 100;
        }
        countToMarkAbsent = (bss.size() * absentPercentage) / 100;
        int intCountToMarkAbsent = countToMarkAbsent.intValue();
        System.out.println("intCountToMarkAbsent = " + intCountToMarkAbsent);
        int temi = 0;
        for (int i = 0; i < intCountToMarkAbsent; i++) {
            BillSession bs;
            int n = ThreadLocalRandom.current().nextInt(0, intCountToMarkAbsent);
            System.out.println("n = " + n);
            if (n < bss.size()) {
                bs = bss.get(n);
                System.out.println("bs marking absent " + bs);
                bs.setAbsent(true);
                bs.setAbsentMarkedAt(new Date());
                bs.setAbsentMarkedUser(null);
                getBillSessionFacade().edit(bs);
            }
        }

        sql = "Select bs From BillSession bs "
                + " where bs.retired=false and "
                + " bs.bill.cancelled=false and "
                + " bs.bill.paidAmount > :paidAmount and "
                + " bs.bill.refunded=false and "
                + " bs.bill.billType in :tbs and "
                + " bs.sessionDate between :ssFrom and :ssTo and "
                + " bs.absentMarkedUser is null and "
                + " bs.absent=true ";
        sql += " order by bs.serialNo";
        hh = new HashMap();
        hh.put("ssFrom", getFromDate());
        hh.put("ssTo", getToDate());
        hh.put("tbs", bts);
        hh.put("paidAmount", 0.0);
        bss = getBillSessionFacade().findBySQL(sql, hh);
        System.out.println("bss.size() = " + bss.size());
        for (BillSession bs : bss) {
            System.out.println("Deleting bs = " + bs);
            deleteBsCascadeAll(bs);
            di++;
        }

        UtilityController.addSuccessMessage("Marked Bulk as Absent");
    }

    @EJB
    private AgentHistoryFacade agentHistoryFacade;
    @EJB
    private PaymentFacade paymentFacade;
    @EJB
    private BillComponentFacade billComponentFacade;
    @EJB
    private PatientEncounterFacade patientEncounterFacade;

    private boolean deleteBsCascadeAll(BillSession bs) {
        System.out.println("bs = " + bs);
        boolean ok = false;
        try {
            Bill db = bs.getBill();
            List<BillFee> dbfs = db.getBillFees();
            List<BillComponent> dbcs = db.getBillComponents();
            List<BillItem> dbis = db.getBillItems();
            List<BillItem> dbies = db.getBillExpenses();
            List<Payment> dps = db.getPayments();
            BillItem dsbi = db.getSingleBillItem();
            BillSession dsbs = db.getSingleBillSession();

            AgentHistory dah = db.getAgentHistory();
            if (dah != null) {
                dah.setBill(null);
                db.setAgentHistory(null);
                agentHistoryFacade.edit(dah);
            }

            if (dsbi != null) {
                dsbi.setBill(null);
                getBillItemFacade().edit(dsbi);
            }

            if (dsbs != null) {
                dsbs.setBill(null);
                dsbs.setBillItem(null);
                getBillSessionFacade().edit(dsbs);
            }

            if (db.getBackwardReferenceBill() != null) {
                Bill brb = db.getBackwardReferenceBill();
                brb.setForwardReferenceBill(null);
                brb.setReferenceBill(null);
                db.setBackwardReferenceBill(null);
                billFacade.edit(brb);
            }

            if (db.getPaidBill() != null) {
                Bill brb = db.getPaidBill();
                brb.setForwardReferenceBill(null);
                brb.setReferenceBill(null);
                db.setBackwardReferenceBill(null);
                billFacade.edit(brb);
                getBillFacade().remove(brb);
            }

            for (BillItem dbie : dbis) {
                dbie.setBill(null);
                dbie.setBillFees(null);
                dbie.setBillSession(null);
                getBillItemFacade().edit(dbie);
            }

            for (Payment dp : dps) {
                dp.setBill(null);
                paymentFacade.edit(dp);
            }

            for (BillItem dbi : dbis) {
                dbi.setBill(null);
                dbi.setBillFees(null);
                dbi.setBillSession(null);
                getBillItemFacade().edit(dbi);
            }

            for (BillFee dbf : dbfs) {
                dbf.setBill(null);
                dbf.setBillItem(null);
                getBillFeeFacade().edit(dbf);
            }

            for (BillComponent dbc : dbcs) {
                dbc.setBill(null);
                dbc.setBillItem(null);
                billComponentFacade.edit(dbc);
            }

            db.setBillComponents(null);
            db.setBillExpenses(null);
            db.setBatchBill(null);
            db.setAgentHistory(null);
            db.setBackwardReferenceBill(null);
            db.setBackwardReferenceBills(null);
            db.setBillItems(null);
            db.setBilledBill(null);
            db.setCancelledBill(null);
            db.setReactivatedBill(null);
            db.setReferenceBill(null);
            db.setRefundBills(null);
            db.setReturnBhtIssueBills(null);
            db.setReturnPreBills(null);
            db.setSingleBillItem(null);
            db.setSingleBillSession(null);
            billFacade.edit(db);

            bs.setBill(null);
            bs.setBillItem(null);
            bs.setItem(null);
            bs.setPaidBillSession(null);
            billSessionFacade.edit(bs);

            BillSession pbs = bs.getPaidBillSession();
            if (pbs != null) {
                pbs.setPaidBillSession(null);
                pbs.setBill(null);
                pbs.setBillItem(null);
                pbs.setPatientEncounter(null);
                getBillSessionFacade().edit(pbs);
                billSessionFacade.edit(pbs);
            }
            BillSession pbrs = bs.getReferenceBillSession();
            if (pbrs != null) {
                pbrs.setPaidBillSession(null);
                pbrs.setBill(null);
                pbrs.setBillItem(null);
                pbrs.setPatientEncounter(null);
                getBillSessionFacade().edit(pbrs);
                billSessionFacade.edit(pbrs);
            }

            PatientEncounter pe = bs.getPatientEncounter();

            if (pe != null) {
                pe.setBillSession(null);
                patientEncounterFacade.edit(pe);
            }

            if (dah != null) {
                agentHistoryFacade.remove(dah);
            }
            if (db.getBackwardReferenceBill() != null) {
                Bill brb = db.getBackwardReferenceBill();
                getBillFacade().remove(brb);
            }

            if (db.getPaidBill() != null) {
                Bill brb = db.getPaidBill();
                getBillFacade().remove(brb);
            }

            for (BillItem dbie : dbis) {
                dbie.setBill(null);
                getBillItemFacade().remove(dbie);
            }

            for (Payment dp : dps) {
                dp.setBill(null);
                paymentFacade.remove(dp);
            }

            if (dsbi != null) {
                getBillItemFacade().remove(dsbi);
            }
            if (dsbs != null) {
                getBillSessionFacade().remove(dsbs);
            }

            for (BillItem dbi : dbis) {
                getBillItemFacade().remove(dbi);
            }

            for (BillFee dbf : dbfs) {
                getBillFeeFacade().remove(dbf);
            }

            for (BillComponent dbc : dbcs) {
                billComponentFacade.remove(dbc);
            }

            if (pbs != null) {
                billSessionFacade.remove(pbs);
            }
            if (pbrs != null) {
                billSessionFacade.remove(pbrs);
            }
            if (pe != null) {
                patientEncounterFacade.remove(pe);
            }

            billFacade.remove(db);

            billSessionFacade.remove(bs);

            System.out.println("successfully deleted");
            ok = true;
            return ok;
        } catch (Exception e) {
            System.out.println("e = " + e);
            ok = false;
            return ok;
        }
    }

    public void markAsPresent() {
        getSelectedBillSession().setAbsent(false);
        getSelectedBillSession().setAbsentMarkedAt(new Date());
        getSelectedBillSession().setAbsentUnmarkedUser(getSessionController().getLoggedUser());
        getBillSessionFacade().edit(getSelectedBillSession());
        UtilityController.addSuccessMessage("Marked as Present");
    }

    public void markAsAbsent() {
        getSelectedBillSession().setAbsent(true);
        getSelectedBillSession().setAbsentMarkedAt(new Date());
        getSelectedBillSession().setAbsentMarkedUser(getSessionController().getLoggedUser());
        getBillSessionFacade().edit(getSelectedBillSession());
        UtilityController.addSuccessMessage("Marked as Absent");
    }

    public void makeNull() {
        speciality = null;
        staff = null;
        selectedServiceSession = null;
        /////////////////////
        serviceSessions = null;
        billSessions = null;
        sessionStartingDate = null;
        consultants = null;
        channelBillController.makeNullSearchData();
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
            ////System.out.println(sql);
            suggestions = getStaffFacade().findBySQL(sql);
        }
        return suggestions;
    }

    public void fillConsultants() {
        String sql;
        Map m = new HashMap();
        m.put("sp", getSpeciality());
        if (getSpeciality() != null) {
            if (getSessionController().getInstitutionPreference().isShowOnlyMarkedDoctors()) {

                sql = " select pi.staff from PersonInstitution pi where pi.retired=false "
                        + " and pi.type=:typ "
                        + " and pi.institution=:ins "
                        + " and pi.staff.speciality=:sp "
                        + " order by pi.staff.person.name ";

                m.put("ins", getSessionController().getInstitution());
                m.put("typ", PersonInstitutionType.Channelling);

            } else {
                sql = "select p from Staff p where p.retired=false and p.speciality=:sp order by p.person.name";
            }

            consultants = getStaffFacade().findBySQL(sql, m);
        } else {
            sql = "select p from Staff p where p.retired=false order by p.person.name";
            consultants = getStaffFacade().findBySQL(sql);
        }
//        //System.out.println("consultants = " + consultants);
        setStaff(null);
    }

    public List<Staff> getSelectedConsultants() {
        System.err.println("Select Specility");
        System.out.println("getSpeciality().getName() = " + getSpeciality().getName());
//        System.out.println("selectText.length() = " + selectTextConsultant.length());
        String sql;
        Map m = new HashMap();

//        //System.out.println("consultants = " + consultants);
        if (selectTextConsultant == null || selectTextConsultant.trim().equals("")) {
            m.put("sp", getSpeciality());
            if (getSpeciality() != null) {
                if (getSessionController().getInstitutionPreference().isShowOnlyMarkedDoctors()) {

                    sql = " select pi.staff from PersonInstitution pi where pi.retired=false "
                            + " and pi.type=:typ "
                            + " and pi.institution=:ins "
                            + " and pi.staff.speciality=:sp ";
                    if (getSessionController().getInstitutionPreference().getApplicationInstitution() == ApplicationInstitution.Ruhuna) {
                        sql += " order by pi.staff.codeInterger , pi.staff.person.name ";
                    } else {
                        sql += " order by pi.staff.person.name ";
                    }
                    m.put("ins", getSessionController().getInstitution());
                    m.put("typ", PersonInstitutionType.Channelling);

                } else {
                    sql = "select p from Staff p where p.retired=false and p.speciality=:sp order by p.person.name";
                }
//                System.out.println("m = " + m);
//                System.out.println("sql = " + sql);
                consultants = getStaffFacade().findBySQL(sql, m);
            }
        } else {
            if (selectTextConsultant.length() > 4) {
                doctorSpecialityController.setSelectText("");
                if (getSessionController().getInstitutionPreference().isShowOnlyMarkedDoctors()) {

                    sql = " select pi.staff from PersonInstitution pi where pi.retired=false "
                            + " and pi.type=:typ "
                            + " and pi.institution=:ins "
                            + " and upper(pi.staff.person.name) like '%" + getSelectTextConsultant().toUpperCase() + "%' "
                            + " order by pi.staff.person.name ";

                    m.put("ins", getSessionController().getInstitution());
                    m.put("typ", PersonInstitutionType.Channelling);
//                    System.out.println("m = " + m);
//                    System.out.println("sql = " + sql);
                    consultants = getStaffFacade().findBySQL(sql, m);

                } else {
                    sql = "select p from Staff p where p.retired=false "
                            + " and upper(p.person.name) like '%" + getSelectTextConsultant().toUpperCase() + "%' "
                            + " order by p.person.name";
                    System.out.println("sql = " + sql);
                    consultants = getStaffFacade().findBySQL(sql);
                }

            } else {
                m.put("sp", getSpeciality());
                if (getSpeciality() != null) {
                    if (getSessionController().getInstitutionPreference().isShowOnlyMarkedDoctors()) {

                        sql = " select pi.staff from PersonInstitution pi where pi.retired=false "
                                + " and pi.type=:typ "
                                + " and pi.institution=:ins "
                                + " and pi.staff.speciality=:sp "
                                + " and upper(pi.staff.person.name) like '%" + getSelectTextConsultant().toUpperCase() + "%' "
                                + " order by pi.staff.person.name ";

                        m.put("ins", getSessionController().getInstitution());
                        m.put("typ", PersonInstitutionType.Channelling);

                    } else {
                        sql = "select p from Staff p where p.retired=false and p.speciality=:sp"
                                + " and upper(p.person.name) like '%" + getSelectTextConsultant().toUpperCase() + "%' "
                                + " order by p.person.name";
                    }
                    System.out.println("m = " + m);
                    System.out.println("sql = " + sql);
                    consultants = getStaffFacade().findBySQL(sql, m);
                }
            }
        }
        if (consultants == null) {
            consultants = new ArrayList<>();
        }

//        if (consultants.size() > 0) {
//            System.out.println("consultants.size() = " + consultants.size());
//            setStaff(consultants.get(0));
//            setSpeciality(getStaff().getSpeciality());
////            generateSessions();
//            generateSessionsOnlyId();
//        } else {
//
//            setStaff(null);
//        }
        return consultants;
    }

    public List<Staff> getConsultants() {
        if (consultants == null) {
            consultants = new ArrayList<>();
        }
        return consultants;
    }

    public void setConsultants(List<Staff> consultants) {
        this.consultants = consultants;
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
        this.speciality = speciality;
    }

//    public void setSpeciality(Speciality speciality) {
//        this.speciality = speciality;
//        fillConsultants();
//        setStaff(null);
//    }
    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

//    public void setStaff(Staff staff) {
////        System.err.println("CLIKED");
//        this.staff = staff;
//        //generateSessions();
//        setSelectedServiceSession(null);
//        serviceSessionLeaveController.setSelectedServiceSession(null);
//        serviceSessionLeaveController.setCurrentStaff(staff);
//    }
    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public void setStaffFacade(StaffFacade staffFacade) {
        this.staffFacade = staffFacade;
    }

    private Double[] fetchFee(Item item, FeeType feeType) {
        String jpql;
        Map m = new HashMap();
        jpql = "Select sum(f.fee),sum(f.ffee) "
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item=:ses "
                + " and f.feeType=:ftp";
        m.put("ses", item);
        m.put("ftp", feeType);
        Object[] obj = getItemFeeFacade().findAggregateModified(jpql, m, TemporalType.TIMESTAMP);

        if (obj == null) {
            Double[] dbl = new Double[2];
            dbl[0] = 0.0;
            dbl[1] = 0.0;
            return dbl;
        }

        Double[] dbl = Arrays.copyOf(obj, obj.length, Double[].class);
//        System.err.println("Fetch Fee Values " + dbl);
        return dbl;
    }

    private List<Object[]> fetchFeeById(Long l) {
        System.out.println("fb In = " + new Date());
        String jpql;
        Map m = new HashMap();
        jpql = "Select f.feeType,sum(f.fee),sum(f.ffee) "
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item.id=:ses "
                + " group by f.feeType ";
        m.put("ses", l);
        List<Object[]> objs = getItemFeeFacade().findAggregates(jpql, m);
        System.out.println("fb Out = " + new Date());
        return objs;
    }

    private double fetchLocalFee(Item item, PaymentMethod paymentMethod) {
        String jpql;
        Map m = new HashMap();
        FeeType[] fts = {FeeType.Service, FeeType.OwnInstitution, FeeType.Staff};
        List<FeeType> feeTypes = Arrays.asList(fts);
        jpql = "Select sum(f.fee)"
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item=:ses ";

        if (paymentMethod == PaymentMethod.Agent) {
            FeeType[] fts1 = {FeeType.Service, FeeType.OwnInstitution, FeeType.Staff, FeeType.OtherInstitution};
            feeTypes = Arrays.asList(fts1);
            jpql += " and f.feeType in :fts1 "
                    + " and f.name!=:name";
            m.put("name", "On-Call Fee");
            m.put("fts1", feeTypes);
        } else {
            if (paymentMethod == PaymentMethod.OnCall) {
                jpql += " and f.feeType in :fts2 ";
                m.put("fts2", feeTypes);
            } else {
                jpql += " and f.feeType in :fts3 "
                        + " and f.name!=:name";
                m.put("name", "On-Call Fee");
                m.put("fts3", feeTypes);
            }
        }
        m.put("ses", item);
//        System.out.println("paymentMethod = " + paymentMethod);
//        System.out.println("feeTypes = " + feeTypes);
//        System.out.println("m = " + m);
        Double obj = getItemFeeFacade().findDoubleByJpql(jpql, m);

        if (obj == null) {
            return 0;
        }

        return obj;
    }

    private double fetchLocalFeeOnlyStaffVat(Item item, PaymentMethod paymentMethod) {
        String jpql;
        Map m = new HashMap();
        List<FeeType> feeTypes = Arrays.asList(new FeeType[]{FeeType.Service, FeeType.OwnInstitution});
        jpql = "Select sum(f.fee)"
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item=:ses ";

        if (paymentMethod == PaymentMethod.Agent) {
            FeeType[] fts1 = {FeeType.Service, FeeType.OwnInstitution, FeeType.OtherInstitution};
            feeTypes = Arrays.asList(fts1);
            jpql += " and f.feeType in :fts1 "
                    + " and f.name!=:name";
            m.put("name", "On-Call Fee");
            m.put("fts1", feeTypes);
        } else {
            if (paymentMethod == PaymentMethod.OnCall) {
                jpql += " and f.feeType in :fts2 ";
                m.put("fts2", feeTypes);
            } else {
                jpql += " and f.feeType in :fts3 "
                        + " and f.name!=:name";
                m.put("name", "On-Call Fee");
                m.put("fts3", feeTypes);
            }
        }
        m.put("ses", item);
        Double obj = getItemFeeFacade().findDoubleByJpql(jpql, m);

        feeTypes = Arrays.asList(new FeeType[]{FeeType.Staff});
        jpql = "Select sum(f.fee)"
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item=:ses ";

        if (paymentMethod == PaymentMethod.Agent) {
            FeeType[] fts1 = {FeeType.Staff};
            feeTypes = Arrays.asList(fts1);
            jpql += " and f.feeType in :fts1 "
                    + " and f.name!=:name";
            m.put("name", "On-Call Fee");
            m.put("fts1", feeTypes);
        } else {
            if (paymentMethod == PaymentMethod.OnCall) {
                jpql += " and f.feeType in :fts2 ";
                m.put("fts2", feeTypes);
            } else {
                jpql += " and f.feeType in :fts3 "
                        + " and f.name!=:name";
                m.put("name", "On-Call Fee");
                m.put("fts3", feeTypes);
            }
        }
        m.put("ses", item);

        Double obj2 = getItemFeeFacade().findDoubleByJpql(jpql, m);

        if (obj == null) {
            obj = 0.0;
        }
        if (obj2 == null) {
            obj2 = 0.0;
        }
        double d = 0.0;
        // who wat to get vat for selected sessions add institution institution to this methord and settle methord
        if (getSessionController().getInstitutionPreference().getApplicationInstitution() == ApplicationInstitution.Cooperative) {
            if (item.isVatable()) {
                d = obj + (obj2 * finalVariables.getVATPercentageWithAmount());
            } else {
                d = obj + obj2;
            }
        } else {
            d = obj + (obj2 * finalVariables.getVATPercentageWithAmount());
        }

        return d;
    }

    private Object[] fetchLocalForiegnFeeById(Long l, PaymentMethod paymentMethod) {
        String jpql;
        Map m = new HashMap();
        FeeType[] fts = {FeeType.Service, FeeType.OwnInstitution, FeeType.Staff};
        List<FeeType> feeTypes = Arrays.asList(fts);
        jpql = "Select sum(f.fee),sum(f.ffee)"
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item.id=:ses ";

        if (paymentMethod == PaymentMethod.Agent) {
            FeeType[] fts1 = {FeeType.Service, FeeType.OwnInstitution, FeeType.Staff, FeeType.OtherInstitution};
            feeTypes = Arrays.asList(fts1);
            jpql += " and f.feeType in :fts1 "
                    + " and f.name!=:name";
            m.put("name", "On-Call Fee");
            m.put("fts1", feeTypes);
        } else {
            if (paymentMethod == PaymentMethod.OnCall) {
                jpql += " and f.feeType in :fts2 ";
                m.put("fts2", feeTypes);
            } else {
                jpql += " and f.feeType in :fts3 "
                        + " and f.name!=:name";
                m.put("name", "On-Call Fee");
                m.put("fts3", feeTypes);
            }
        }
        m.put("ses", l);
//        System.out.println("paymentMethod = " + paymentMethod);
//        System.out.println("feeTypes = " + feeTypes);
//        System.out.println("m = " + m);
        Object[] obj = getBillFacade().findAggregateModified(jpql, m, TemporalType.DATE);

        return obj;
    }

    private double fetchForiegnFee(Item item, PaymentMethod paymentMethod) {
        String jpql;
        Map m = new HashMap();
        FeeType[] fts = {FeeType.Service, FeeType.OwnInstitution, FeeType.Staff};
        List<FeeType> feeTypes = Arrays.asList(fts);
        jpql = "Select sum(f.ffee)"
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item=:ses ";

        if (paymentMethod == PaymentMethod.Agent) {
            FeeType[] fts1 = {FeeType.Service, FeeType.OwnInstitution, FeeType.Staff, FeeType.OtherInstitution};
            feeTypes = Arrays.asList(fts1);
            jpql += " and f.feeType in :fts1 "
                    + " and f.name!=:name";
            m.put("name", "On-Call Fee");
            m.put("fts1", feeTypes);
        } else {
            if (paymentMethod == PaymentMethod.OnCall) {
                jpql += " and f.feeType in :fts2 ";
                m.put("fts2", feeTypes);
            } else {
                jpql += " and f.feeType in :fts3 "
                        + " and f.name!=:name";
                m.put("name", "On-Call Fee");
                m.put("fts3", feeTypes);
            }
        }
        m.put("ses", item);
//        System.out.println("paymentMethod = " + paymentMethod);
//        System.out.println("feeTypes = " + feeTypes);
//        System.out.println("m = " + m);
        Double obj = getItemFeeFacade().findDoubleByJpql(jpql, m);

        if (obj == null) {
            return 0;
        }

        return obj;
    }

    private double fetchForiegnFeeOnlyStaffVat(Item item, PaymentMethod paymentMethod) {
        String jpql;
        Map m = new HashMap();
        List<FeeType> feeTypes = Arrays.asList(new FeeType[]{FeeType.Service, FeeType.OwnInstitution});
        jpql = "Select sum(f.ffee)"
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item=:ses ";

        if (paymentMethod == PaymentMethod.Agent) {
            FeeType[] fts1 = {FeeType.Service, FeeType.OwnInstitution, FeeType.OtherInstitution};
            feeTypes = Arrays.asList(fts1);
            jpql += " and f.feeType in :fts1 "
                    + " and f.name!=:name";
            m.put("name", "On-Call Fee");
            m.put("fts1", feeTypes);
        } else {
            if (paymentMethod == PaymentMethod.OnCall) {
                jpql += " and f.feeType in :fts2 ";
                m.put("fts2", feeTypes);
            } else {
                jpql += " and f.feeType in :fts3 "
                        + " and f.name!=:name";
                m.put("name", "On-Call Fee");
                m.put("fts3", feeTypes);
            }
        }
        m.put("ses", item);
        Double obj = getItemFeeFacade().findDoubleByJpql(jpql, m);

        feeTypes = Arrays.asList(new FeeType[]{FeeType.Staff});
        jpql = "Select sum(f.ffee)"
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item=:ses ";

        if (paymentMethod == PaymentMethod.Agent) {
            FeeType[] fts1 = {FeeType.Staff};
            feeTypes = Arrays.asList(fts1);
            jpql += " and f.feeType in :fts1 "
                    + " and f.name!=:name";
            m.put("name", "On-Call Fee");
            m.put("fts1", feeTypes);
        } else {
            if (paymentMethod == PaymentMethod.OnCall) {
                jpql += " and f.feeType in :fts2 ";
                m.put("fts2", feeTypes);
            } else {
                jpql += " and f.feeType in :fts3 "
                        + " and f.name!=:name";
                m.put("name", "On-Call Fee");
                m.put("fts3", feeTypes);
            }
        }
        m.put("ses", item);

        Double obj2 = getItemFeeFacade().findDoubleByJpql(jpql, m);

        if (obj == null) {
            obj = 0.0;
        }
        if (obj2 == null) {
            obj2 = 0.0;
        }
        double d = 0.0;
        // who wat to get vat for selected sessions add institution institution to this methord and settle methord
        if (getSessionController().getInstitutionPreference().getApplicationInstitution() == ApplicationInstitution.Cooperative) {
            if (item.isVatable()) {
                d = obj + (obj2 * finalVariables.getVATPercentageWithAmount());
            } else {
                d = obj + obj2;
            }
        } else {
            d = obj + (obj2 * finalVariables.getVATPercentageWithAmount());
        }

        return d;
    }

    private List<ItemFee> fetchFee(Item item) {
        String jpql;
        Map m = new HashMap();
        jpql = "Select f "
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item=:ses ";
        m.put("ses", item);
        List<ItemFee> list = getItemFeeFacade().findBySQL(jpql, m, TemporalType.TIMESTAMP);
//        System.err.println("Fetch Fess " + list.size());
        return list;
    }

    private List<ItemFee> fetchItemFeeById(Long l) {
        String jpql;
        Map m = new HashMap();
        jpql = "Select f "
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item.id=:ses ";
        m.put("ses", l);
        List<ItemFee> list = getItemFeeFacade().findBySQL(jpql, m, TemporalType.TIMESTAMP);
//        System.err.println("Fetch Fess " + list.size());
        return list;
    }

    public void calculateFee(List<ServiceSession> lstSs, PaymentMethod paymentMethod) {
        for (ServiceSession ss : lstSs) {
            Double[] dbl = fetchFee(ss, FeeType.OwnInstitution);
            ss.setHospitalFee(dbl[0]);
            ss.setHospitalFfee(dbl[1]);
            dbl = fetchFee(ss, FeeType.Staff);
            ss.setProfessionalFee(dbl[0]);
            ss.setProfessionalFfee(dbl[1]);
//            System.err.println("1111");
            dbl = fetchFee(ss, FeeType.Tax);
//            System.err.println("2222");
            ss.setTaxFee(dbl[0]);
            ss.setTaxFfee(dbl[1]);
            ss.setTotalFee(fetchLocalFee(ss, paymentMethod));
            ss.setTotalFfee(fetchForiegnFee(ss, paymentMethod));
            ss.setItemFees(fetchFee(ss));
        }
    }

    public void calculateFeeBySessionIdList(List<Long> lstSs, PaymentMethod paymentMethod) {
        System.err.println("Cal Fee IN = " + new Date());
        for (Long ss : lstSs) {
//            System.err.println("Cal Fee  in Time  = " + new Date() + " SS id=" + ss);
            ServiceSession session = getServiceSessionFacade().find(ss);
            List<Object[]> objs = fetchFeeById(ss);
            for (Object[] o : objs) {
                FeeType ft = (FeeType) o[0];
                if (ft == FeeType.OwnInstitution) {
                    session.setHospitalFee((double) o[1]);
                    session.setHospitalFfee((double) o[2]);
                    continue;
                }
                if (ft == FeeType.Staff) {
                    session.setProfessionalFee((double) o[1]);
                    session.setProfessionalFfee((double) o[2]);
                    continue;
                }
                if (ft == FeeType.Staff) {
                    session.setTaxFee((double) o[1]);
                    session.setTaxFfee((double) o[2]);
                }
            }
            Object[] ob = fetchLocalForiegnFeeById(ss, paymentMethod);
            session.setTotalFee((double) ob[0]);
            session.setTotalFfee((double) ob[1]);
            session.setItemFees(fetchItemFeeById(ss));
        }
        System.err.println("Cal Fee Out = " + new Date());
    }

    public void calculateFeeBooking(List<ServiceSession> lstSs, PaymentMethod paymentMethod) {
        for (ServiceSession ss : lstSs) {
            Double[] dbl = fetchFee(ss.getOriginatingSession(), FeeType.OwnInstitution);
            ss.setHospitalFee(dbl[0]);
            ss.setHospitalFfee(dbl[1]);
            //For Settle bill
            ss.getOriginatingSession().setHospitalFee(dbl[0]);
            ss.getOriginatingSession().setHospitalFfee(dbl[1]);
            //For Settle bill
            dbl = fetchFee(ss.getOriginatingSession(), FeeType.Staff);
            ss.setProfessionalFee(dbl[0]);
            ss.setProfessionalFfee(dbl[1]);
            //For Settle bill
            ss.getOriginatingSession().setProfessionalFee(dbl[0]);
            ss.getOriginatingSession().setProfessionalFfee(dbl[1]);
            //For Settle bill
            dbl = fetchFee(ss.getOriginatingSession(), FeeType.Tax);
            ss.setTaxFee(dbl[0]);
            ss.setTaxFfee(dbl[1]);
            //For Settle bill
            ss.getOriginatingSession().setTaxFee(dbl[0]);
            ss.getOriginatingSession().setTaxFfee(dbl[1]);
            //For Settle bill
            ss.setTotalFee(fetchLocalFee(ss.getOriginatingSession(), paymentMethod));
            ss.setTotalFfee(fetchForiegnFee(ss.getOriginatingSession(), paymentMethod));
            ss.setItemFees(fetchFee(ss.getOriginatingSession()));
            //For Settle bill
            ss.getOriginatingSession().setTotalFee(fetchLocalFee(ss.getOriginatingSession(), paymentMethod));
            ss.getOriginatingSession().setTotalFfee(fetchForiegnFee(ss.getOriginatingSession(), paymentMethod));
            ss.getOriginatingSession().setItemFees(fetchFee(ss.getOriginatingSession()));
            //For Settle bill
        }
    }

    public void calculateFeeBookingNew(List<ServiceSession> lstSs, PaymentMethod paymentMethod) {
        int rowIndex = 0;
        for (ServiceSession ss : lstSs) {
            ss.setDisplayCount(channelBean.getBillSessionsCount(ss, ss.getSessionDate()));
            ss.setTransDisplayCountWithoutCancelRefund(channelBean.getBillSessionsCountWithOutCancelRefund(ss, ss.getSessionDate()));
            ss.setTransCreditBillCount(channelBean.getBillSessionsCountCrditBill(ss, ss.getSessionDate()));
            ss.setTransRowNumber(rowIndex++);
//            System.err.println("Time D.A. in = " + new Date());
//            checkDoctorArival(ss);
//            System.err.println("Time D.A. Out = " + new Date());

            Double[] dbl = fetchFee(ss.getOriginatingSession(), FeeType.OwnInstitution);
            ss.setHospitalFee(dbl[0]);
            ss.setHospitalFfee(dbl[1]);
            //For Settle bill
            ss.getOriginatingSession().setHospitalFee(dbl[0]);
            ss.getOriginatingSession().setHospitalFfee(dbl[1]);
            //For Settle bill
            dbl = fetchFee(ss.getOriginatingSession(), FeeType.Staff);
            ss.setProfessionalFee(dbl[0]);
            ss.setProfessionalFfee(dbl[1]);
            //For Settle bill
            ss.getOriginatingSession().setProfessionalFee(dbl[0]);
            ss.getOriginatingSession().setProfessionalFfee(dbl[1]);
            //For Settle bill
            dbl = fetchFee(ss.getOriginatingSession(), FeeType.Tax);
            ss.setTaxFee(dbl[0]);
            ss.setTaxFfee(dbl[1]);
            //For Settle bill
            ss.getOriginatingSession().setTaxFee(dbl[0]);
            ss.getOriginatingSession().setTaxFfee(dbl[1]);
            //For Settle bill
            if (getSessionController().getInstitutionPreference().getApplicationInstitution() == ApplicationInstitution.Arogya) {
                ss.setTotalFee(fetchLocalFee(ss.getOriginatingSession(), paymentMethod));
                ss.setTotalFfee(fetchForiegnFee(ss.getOriginatingSession(), paymentMethod));
                ss.getOriginatingSession().setTotalFee(fetchLocalFee(ss.getOriginatingSession(), paymentMethod));
                ss.getOriginatingSession().setTotalFfee(fetchForiegnFee(ss.getOriginatingSession(), paymentMethod));

                if (ss.getOriginatingSession().isVatable()) {
                    //all Bill
//                    ss.setTotalFee(ss.getTotalFee() * finalVariables.getVATPercentageWithAmount());
//                    ss.getOriginatingSession().setTotalFee(ss.getOriginatingSession().getTotalFfee() * finalVariables.getVATPercentageWithAmount());

                    //only Doc Fee
                    ss.setTotalFee(fetchLocalFeeOnlyStaffVat(ss.getOriginatingSession(), paymentMethod));
                    ss.getOriginatingSession().setTotalFee(fetchLocalFeeOnlyStaffVat(ss.getOriginatingSession(), paymentMethod));
                }
            } else {
                ss.setTotalFee(fetchLocalFeeOnlyStaffVat(ss.getOriginatingSession(), paymentMethod));
                ss.setTotalFfee(fetchForiegnFeeOnlyStaffVat(ss.getOriginatingSession(), paymentMethod));
                ss.getOriginatingSession().setTotalFee(fetchLocalFeeOnlyStaffVat(ss.getOriginatingSession(), paymentMethod));
                ss.getOriginatingSession().setTotalFfee(fetchForiegnFeeOnlyStaffVat(ss.getOriginatingSession(), paymentMethod));
            }
            ss.setItemFees(fetchFee(ss.getOriginatingSession()));
            //For Settle bill
            ss.getOriginatingSession().setItemFees(fetchFee(ss.getOriginatingSession()));
            //For Settle bill
        }
    }

    public void generateSessions() {
        serviceSessions = new ArrayList<>();
        String sql;
        Map m = new HashMap();
        m.put("staff", getStaff());
        m.put("class", ServiceSession.class);
        System.err.println("original Time in = " + new Date());

        System.err.println("Time stage 1 = " + new Date());
        if (staff != null) {
            sql = "Select s From ServiceSession s "
                    + " where s.retired=false "
                    + " and s.staff=:staff "
                    + " and s.originatingSession is null "
                    + " and type(s)=:class "
                    + " order by s.sessionWeekday,s.startingTime ";
            System.out.println("m = " + m);
            System.out.println("sql = " + sql);
            List<ServiceSession> tmp = new ArrayList<>();
            System.err.println("Time stage 2.1 = " + new Date());
            tmp = getServiceSessionFacade().findBySQL(sql, m);

            for (ServiceSession ss : tmp) {
                ss.getStaff();
                ss.getDepartment();

            }

            System.err.println("Time stage 2.2 = " + new Date());
            System.err.println("Fetch Sessions " + tmp.size());
            calculateFee(tmp, channelBillController.getPaymentMethod());
            System.err.println("Time stage 3 Calculate = " + new Date());
            serviceSessions = getChannelBean().generateDailyServiceSessionsFromWeekdaySessionsNew(tmp, sessionStartingDate);
            System.err.println("Time stage 4 = " + new Date());
            generateSessionEvents(serviceSessions);
            checkDoctorArival(serviceSessions);
        }
    }

    public void generateSessionsOnlyId() {
        System.err.println("Time in = " + new Date());
        serviceSessions = new ArrayList<>();
        String sql;
        Map m = new HashMap();
        m.put("staff", getStaff());
        m.put("class", ServiceSession.class);
        m.put("nd", new Date());
        System.err.println("Time stage 1 = " + new Date());
        if (staff != null) {
            sql = "Select s.id From ServiceSession s "
                    + " where s.retired=false "
                    + " and s.staff=:staff "
                    + " and s.originatingSession is null"
                    + " and ((s.sessionWeekday is null and s.sessionDate >=:nd)or(s.sessionWeekday is not null and s.sessionDate is null)) "
                    + " and type(s)=:class "
                    + " order by s.sessionWeekday,s.startingTime ";
            System.out.println("Consultant = " + getStaff().getPerson().getName());
            System.out.println("m = " + m);
            System.out.println("sql = " + sql);
            List<Long> tmp = new ArrayList<>();
            System.err.println("Time stage 2.1 = " + new Date());
            tmp = getServiceSessionFacade().findLongList(sql, m, TemporalType.DATE);
            System.err.println("Time stage 2.2 = " + new Date());

            System.err.println("Fetch Original Sessions = " + tmp.size());
            System.err.println("Time stage 3.1 = " + new Date());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
            System.err.println("Time stage 3.2 = " + new Date());

            System.err.println("Time stage 4.1 = " + new Date());
            serviceSessions = getChannelBean().generateDailyServiceSessionsFromWeekdaySessionsNewByServiceSessionId(tmp, sessionStartingDate);
            System.err.println("Fetch Created Sessions " + serviceSessions.size());
            System.err.println("Time stage 4.2 = " + new Date());

            System.err.println("Time stage 5 = " + new Date());
//            generateSessionEvents(serviceSessions);
            System.err.println("Time stage 6 = " + new Date());
        }
    }

    public void generateSessionsOnlyIdNew() {
//        System.err.println("Time in = " + new Date());
        serviceSessions = new ArrayList<>();
        String sql;
        Map m = new HashMap();
        m.put("staff", getStaff());
        m.put("class", ServiceSession.class);
        m.put("nd", new Date());
//        System.err.println("Time stage 1 = " + new Date());
        if (staff != null) {
//            System.err.println("Time stage 4.1 = " + new Date());
            serviceSessions = getChannelBean().generateDailyServiceSessionsFromWeekdaySessionsNewByServiceSessionIdNew(staff, sessionStartingDate);
            System.err.println("Fetch Created Sessions " + serviceSessions.size());
//            System.err.println("Time stage 4.2 = " + new Date());
        }
        if (getSessionController().getLoggedUser().getWebUserPerson() != null) {
            System.err.println("User = " + getSessionController().getLoggedUser().getWebUserPerson().getName());
        }
    }

    public void generateSessionEvents(List<ServiceSession> sss) {
        eventModel = new DefaultScheduleModel();
        for (ServiceSession s : sss) {
            ChannelScheduleEvent e = new ChannelScheduleEvent();
            e.setServiceSession(s);
            e.setTitle(s.getName());
            e.setStartDate(s.getTransStartTime());
            e.setEndDate(s.getTransEndTime());
            eventModel.addEvent(e);
            checkDoctorArival(s);
        }
    }

    public void checkDoctorArival(ServiceSession s) {
//        System.out.println("s.getName() = " + s.getName());
        s.setArival(findArrivals(s));
//        System.out.println("s.getArival() = " + s.getArival());
    }

    public void checkDoctorArival(List<ServiceSession> sss) {
        for (ServiceSession s : sss) {
            System.out.println("s.getName() = " + s.getName());
            s.setArival(findArrivals(s));
            System.out.println("s.getArival() = " + s.getArival());
        }
    }

    public void onEventSelect(SelectEvent selectEvent) {
        event = (ChannelScheduleEvent) selectEvent.getObject();
        selectedServiceSession = event.getServiceSession();
        fillBillSessions();
    }

    public void generateSessionsFutureBooking(SelectEvent event) {
        date = null;
        date = ((Date) event.getObject());
        serviceSessions = new ArrayList<>();
        Map m = new HashMap();

        Date currenDate = new Date();
        if (getDate().before(currenDate)) {
            UtilityController.addErrorMessage("Please Select Future Date");
            return;
        }

        String sql = "";

        if (staff != null) {
            Calendar c = Calendar.getInstance();
            c.setTime(getDate());
            int wd = c.get(Calendar.DAY_OF_WEEK);

            sql = "Select s From ServiceSession s "
                    + " where s.retired=false "
                    + " and s.staff=:staff "
                    + " and s.sessionWeekday=:wd ";

            m.put("staff", getStaff());
            m.put("wd", wd);
            List<ServiceSession> tmp = getServiceSessionFacade().findBySQL(sql, m);
            calculateFee(tmp, channelBillController.getPaymentMethod());//check work future bokking
            serviceSessions = getChannelBean().generateServiceSessionsForSelectedDate(tmp, date);
        }

        billSessions = new ArrayList<>();
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
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
        return billSessions;
    }

//    public void fillBillSessions(SelectEvent event) {
//        selectedBillSession = null;
//        selectedServiceSession = ((ServiceSession) event.getObject());
//
//        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
//        List<BillType> bts = Arrays.asList(billTypes);
//
//        String sql = "Select bs From BillSession bs "
//                + " where bs.retired=false"
//                + " and bs.serviceSession=:ss "
//                + " and bs.bill.billType in :bt"
//                + " and type(bs.bill)=:class "
//                + " and bs.sessionDate= :ssDate "
//                + " order by bs.serialNo ";
//        HashMap hh = new HashMap();
//        hh.put("bt", bts);
//        hh.put("class", BilledBill.class);
//        hh.put("ssDate", getSelectedServiceSession().getSessionAt());
//        hh.put("ss", getSelectedServiceSession());
//        billSessions = getBillSessionFacade().findBySQL(sql, hh, TemporalType.DATE);
//        System.out.println("hh = " + hh);
//        System.out.println("getSelectedServiceSession().isTransLeave() = " + getSelectedServiceSession().isTransLeave());
//        if (getSelectedServiceSession().isTransLeave()) {
//            billSessions=null;
//        }
//        System.out.println("billSessions" + billSessions);
//
//    }
    public void findArrivals() {

        String sql = "Select bs From ArrivalRecord bs "
                + " where bs.retired=false"
                + " and bs.serviceSession=:ss "
                + " and bs.sessionDate= :ssDate ";
        HashMap hh = new HashMap();
        hh.put("ssDate", getSelectedServiceSession().getSessionDate());
        hh.put("ss", getSelectedServiceSession());
        arrivalRecord = (ArrivalRecord) fpFacade.findFirstBySQL(sql, hh);
    }

    public Boolean findArrivals(ServiceSession ss) {

        String sql = "Select bs From ArrivalRecord bs "
                + " where bs.retired=false"
                + " and bs.serviceSession.id=:ss "
                + " and bs.sessionDate=:ssDate ";
        HashMap hh = new HashMap();
        hh.put("ssDate", ss.getSessionDate());
        hh.put("ss", ss.getId());
        arrivalRecord = (ArrivalRecord) fpFacade.findFirstBySQL(sql, hh);

        if (arrivalRecord != null) {
            if (arrivalRecord.isApproved()) {
                return true;
            } else {
                return false;
            }
        }
        return null;
    }

    public void sendSmsDoctorArived(ServiceSession ss) {
        System.out.println("ss.getSessionAt() = " + ss.getSessionAt());
        System.out.println("ss.getSessionDate() = " + ss.getSessionDate());
        System.out.println("ss.getSessionTime() = " + ss.getSessionTime());
        System.out.println("ss.getStartingTime() = " + ss.getStartingTime());
        System.out.println("ss.getEndingTime() = " + ss.getEndingTime());

        Calendar cal = Calendar.getInstance();
        cal.setTime(ss.getStartingTime());
        cal.add(Calendar.HOUR, 3);
        System.out.println("cal.getTime() = " + cal.getTime());

        List<ServiceSession> list = fetchServiceSessionsForTimeRange(ss.getStaff(), ss.getSessionDate(), ss.getStartingTime(), cal.getTime());
        List<BillSession> bSessions = new ArrayList<>();
        for (ServiceSession s : list) {
            bSessions.addAll(fillBillSessions(s));
            System.out.println("billSessions = " + bSessions.size());
        }

        String msg = "Dear Sir/Madam,\n"
                + ss.getStaff().getPerson().getName() + " has arrived.\n"
                + "** Now you can channel your doctor online on www.ruhunuhospitl.lk **";
        System.out.println("ss.getStaff().getPerson().getName() = " + ss.getStaff().getPerson().getName().length());
        System.out.println("msg.length() = " + msg.length());
//        fillBillSessions();
        for (BillSession bs : bSessions) {
            if (bs.getBill().isCancelled() || bs.getBill().isRefunded()) {
                System.out.println("bs.getBill().getPatient().getPerson().getPhone() = " + bs.getBill().getPatient().getPerson().getPhone());
                continue;
            }
            smsController.sendSmsToNumberList(bs.getBill().getPatient().getPerson().getPhone(), getSessionController().getUserPreference().getApplicationInstitution(), msg, bs.getBill(), SmsType.ChannelDoctorAraival);
        }
    }

    public void sendSmsToinformLeave() {
        String msg = "Dear Sir/Madam,\n"
                + selectedServiceSession.getStaff().getPerson().getName() + " is Leave Today."
                + "Thank you for using Ruhunu Hospital services.";
        System.out.println("msg.length() = " + msg.length());
        //fillBillSessions();
        System.out.println("billSessions.size() = " + billSessions.size());
        for (BillSession bs : billSessions) {
            if (bs.getBill().isCancelled() || bs.getBill().isRefunded()) {
                System.out.println("bs.getBill().getPatient().getPerson().getPhone() = " + bs.getBill().getPatient().getPerson().getPhone());
                continue;
            }
//            smsController.sendSmsToNumberList(bs.getBill().getPatient().getPerson().getPhone(), getSessionController().getUserPreference().getApplicationInstitution(), msg,bs.getBill());
        }
    }

    public List<ServiceSession> fetchServiceSessionsForTimeRange(Staff s, Date date, Date ft, Date tt) {
        String sql;
        Map m = new HashMap();
        List<ServiceSession> tmp = new ArrayList<>();
        sql = "Select s From ServiceSession s where s.retired=false "
                + " and s.staff=:staff "
                + " and s.originatingSession is not null "
                + " and s.sessionDate=:d "
                + " and s.startingTime between :ft and :tt "
                + " and type(s)=:class "
                + " order by s.sessionDate,s.startingTime ";
        m.put("d", date);
        m.put("ft", ft);
        m.put("tt", tt);
        m.put("staff", s);
        m.put("class", ServiceSession.class);
        try {
            tmp = getServiceSessionFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);
            System.out.println("tmp.size() = " + tmp.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tmp;
    }

    public void markAsArrived() {
        if (selectedServiceSession == null) {
            System.out.println("selectedServiceSession is null");
            return;
        }
        if (selectedServiceSession.getSessionDate() == null) {
            System.out.println("selectedServiceSession.date is null");
            return;
        }
        if (commonFunctions.getEndOfDay(selectedServiceSession.getSessionDate()).getTime() != commonFunctions.getEndOfDay(new Date()).getTime()) {
            JsfUtil.addErrorMessage("You Can Mark Only Today Arrivals Only");
            return;
        }
        if (arrivalRecord == null) {
            arrivalRecord = new ArrivalRecord();
            arrivalRecord.setSessionDate(selectedServiceSession.getSessionDate());
            arrivalRecord.setServiceSession(selectedServiceSession);
            arrivalRecord.setCreatedAt(new Date());
            arrivalRecord.setCreater(sessionController.getLoggedUser());
            fpFacade.create(arrivalRecord);
            //
            if (getSessionController().getInstitutionPreference().isChannelDoctorArivalMsgSend()) {
                sendSmsDoctorArived(selectedServiceSession);
            }
        }
        arrivalRecord.setRecordTimeStamp(new Date());
        arrivalRecord.setApproved(false);
        fpFacade.edit(arrivalRecord);
    }

    public void markAsLeft() {
        if (selectedServiceSession == null) {
            System.out.println("selectedServiceSession is null");
            return;
        }
        if (selectedServiceSession.getSessionDate() == null) {
            System.out.println("selectedServiceSession.date is null");
            return;
        }
        if (arrivalRecord == null) {
            arrivalRecord = new ArrivalRecord();
            arrivalRecord.setSessionDate(selectedServiceSession.getSessionDate());
            arrivalRecord.setServiceSession(selectedServiceSession);
            arrivalRecord.setCreatedAt(new Date());
            arrivalRecord.setCreater(sessionController.getLoggedUser());
            fpFacade.create(arrivalRecord);
        }

        arrivalRecord.setApproved(true);
        arrivalRecord.setApprovedAt(new Date());
        arrivalRecord.setApprover(sessionController.getLoggedUser());
        fpFacade.edit(arrivalRecord);
    }

    public void fillBillSessions() {
        selectedBillSession = null;
//        selectedServiceSession = ((ServiceSession) event.getObject());

        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);

        String sql = "Select bs From BillSession bs "
                + " where bs.retired=false"
                + " and bs.serviceSession=:ss "
                + " and bs.bill.billType in :bt"
                + " and type(bs.bill)=:class "
                + " and bs.sessionDate= :ssDate "
                + " order by bs.serialNo ";
        HashMap hh = new HashMap();
        hh.put("bt", bts);
        hh.put("class", BilledBill.class);
        hh.put("ssDate", getSelectedServiceSession().getSessionDate());
        hh.put("ss", getSelectedServiceSession());
        billSessions = getBillSessionFacade().findBySQL(sql, hh, TemporalType.DATE);

    }

    public List<BillSession> fillBillSessions(ServiceSession ss) {

        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);

        String sql = "Select bs From BillSession bs "
                + " where bs.retired=false"
                + " and bs.serviceSession=:ss "
                + " and bs.bill.billType in :bt"
                + " and type(bs.bill)=:class "
                + " and bs.sessionDate= :ssDate "
                + " order by bs.serialNo ";
        HashMap hh = new HashMap();
        hh.put("bt", bts);
        hh.put("class", BilledBill.class);
        hh.put("ssDate", ss.getSessionDate());
        hh.put("ss", ss);
        return getBillSessionFacade().findBySQL(sql, hh, TemporalType.DATE);

    }

    public void fillAbsentBillSessions(SelectEvent event) {
        selectedBillSession = null;
        selectedServiceSession = ((ServiceSession) event.getObject());

        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);

        String sql = "Select bs From BillSession bs "
                + " where bs.retired=false"
                + " and bs.serviceSession=:ss "
                + " and bs.bill.billType in :bt "
                + " and bs.absent=true "
                + " and type(bs.bill)=:class "
                + " and bs.sessionDate= :ssDate "
                + " order by bs.serialNo ";
        HashMap hh = new HashMap();
        hh.put("bt", bts);
        hh.put("class", BilledBill.class);
        hh.put("ssDate", getSelectedServiceSession().getSessionAt());
        hh.put("ss", getSelectedServiceSession());
        billSessions = getBillSessionFacade().findBySQL(sql, hh, TemporalType.DATE);
        //absentCount=billSessions.size();

    }

    public String paySelectedDoctor() {
        if (getSpeciality() == null) {
            JsfUtil.addErrorMessage("Please Select Specility And Staff");
            return "";
        }
        if (getStaff() == null) {
            JsfUtil.addErrorMessage("Please Select Staff");
            return "";
        }
        channelStaffPaymentBillController.makenull();
        channelStaffPaymentBillController.setSpeciality(getSpeciality());
        channelStaffPaymentBillController.setCurrentStaff(getStaff());
        channelStaffPaymentBillController.fillSessions();
        channelStaffPaymentBillController.setConsiderDate(true);
        channelStaffPaymentBillController.calculateDueFees();

        return "/channel/channel_payment_staff_bill";

    }

    public void onEditItem(RowEditEvent event) {
        ServiceSession tmp = (ServiceSession) event.getObject();
        ServiceSession ss = getServiceSessionFacade().find(tmp.getId());
        if (ss.getMaxNo() != tmp.getMaxNo() || ss.getStartingNo() != tmp.getStartingNo() || ss.getSessionTime() != tmp.getStartingTime() || ss.isRetired() != tmp.isRetired()) {
            tmp.setEditedAt(new Date());
            tmp.setEditer(getSessionController().getLoggedUser());
        }
        getServiceSessionFacade().edit(tmp);
    }

    public void listnerStaffListForRowSelect() {
        getSelectedConsultants();
    }

    public void listnerStaffListForRowSelectNew() {
        serviceSessions = new ArrayList<>();
        listnerStaffListForRowSelect();
        listnerClearSelectedServiceSession();
    }

    public void clearServiceSessions() {
        serviceSessions = new ArrayList<>();
    }

    public void listnerServiceSessionListForRowSelectNew() {
        generateSessionsOnlyIdNew();
//        generateSessionsOnlyId(); before Optimize
        listnerClearSelectedServiceSession();
    }

    public void listnerBillSessionListForRowSelectNew() {
        fillBillSessions();
        listnerClearSelectedBillSession();
        if (getSessionController().getInstitutionPreference().getApplicationInstitution() == ApplicationInstitution.Cooperative
                && getSelectedServiceSession().getOriginatingSession().getForBillType() == BillType.XrayScan) {
            getSelectedServiceSession().getOriginatingSession().setItemFees(fetchFee(getSelectedServiceSession().getOriginatingSession()));
        }
    }

    public void listnerStaffRowSelect() {
        getSelectedConsultants();
        setSelectedServiceSession(null);
        serviceSessionLeaveController.setSelectedServiceSession(null);
        serviceSessionLeaveController.setCurrentStaff(staff);
    }

    public void listnerSessionRowSelect() {
        for (ServiceSession ss : serviceSessions) {
            if (ss.getSessionText().toLowerCase().contains(selectTextSession.toLowerCase())) {
                selectedServiceSession = ss;
            }
        }
    }

    public void listnerStaffListForSpecilitySelectedText() {
        if (doctorSpecialityController.getSelectedItems().size() > 0) {
            setSpeciality(doctorSpecialityController.getSelectedItems().get(0));
            listnerStaffListForRowSelect();
        }
    }

    public void listnerClearSelectedServiceSession() {
        selectedServiceSession = null;
        billSessions = null;
        selectedBillSession = null;
        getChannelCancelController().makeNull();
        getChannelBillController().setBillSession(null);
    }

    public void listnerClearSelectedBillSession() {
        selectedBillSession = null;
        getChannelBillController().setBillSession(null);
    }

    public void viewBill(BillSession bs) {
//        setSpeciality(bs.getServiceSession().getStaff().getSpeciality());
//        System.out.println("++++getSpeciality().getName() = " + getSpeciality().getName());

//        getSelectedConsultants();
//        setSpeciality(bs.getServiceSession().getStaff().getSpeciality());
//        setStaff(bs.getServiceSession().getStaff());
//        System.out.println("++++bs.getServiceSession().getStaff().getName() = " + bs.getServiceSession().getStaff().getPerson().getName());
//        System.out.println("++++getStaff().getPerson().getName() = " + getStaff().getPerson().getName());
//        generateSessionsOnlyId();
//        setSelectedServiceSession(bs.getServiceSession());
//        System.out.println("++++bs.getServiceSession() = " + bs.getServiceSession());
//        System.out.println("++++getSelectedServiceSession() = " + getSelectedServiceSession());
//        fillBillSessions();
        System.out.println("++++channelBillController.getBillSession() = " + channelBillController.getBillSession());
        System.out.println("++++channelBillController.getBillSessionTmp() = " + channelBillController.getBillSessionTmp());
        setSelectedBillSession(bs);
//        getChannelBillController().setBillSession(bs);
        System.out.println("++++bs = " + bs);
        System.out.println("++++getSelectedBillSession() = " + getSelectedBillSession());
        System.out.println("++++channelBillController.getBillSession() = " + channelBillController.getBillSession());
        System.out.println("++++channelBillController.getBillSessionTmp() = " + channelBillController.getBillSessionTmp());
        channelBillController.listnerSetBillSession(bs);
        System.out.println("++++channelBillController.getBillSession() = " + channelBillController.getBillSession());
        System.out.println("++++channelBillController.getBillSessionTmp() = " + channelBillController.getBillSessionTmp());
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
        getChannelBillController().makeNull();
        getChannelBillController().setBillSession(selectedBillSession);
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

    public Date getChannelDay() {
        return channelDay;
    }

    public void setChannelDay(Date channelDay) {
        this.channelDay = channelDay;
    }

    public int getSerealNo() {
        return serealNo;
    }

    public void setSerealNo(int serealNo) {
        this.serealNo = serealNo;
    }

    public ScheduleModel getEventModel() {
        if (eventModel == null) {
            eventModel = new DefaultScheduleModel();
        }
        return eventModel;
    }

    public void setEventModel(ScheduleModel eventModel) {
        this.eventModel = eventModel;
    }

    public ChannelScheduleEvent getEvent() {
        if (event == null) {
            event = new ChannelScheduleEvent();
        }
        return event;
    }

    public void setEvent(ChannelScheduleEvent event) {
        this.event = event;
    }

    public Date getSessionStartingDate() {
        if (sessionStartingDate == null) {
            sessionStartingDate = new Date();
        }
        return sessionStartingDate;
    }

    public void setSessionStartingDate(Date sessionStartingDate) {
        this.sessionStartingDate = sessionStartingDate;
    }

    public String getSelectTextSpeciality() {
        return selectTextSpeciality;
    }

    public void setSelectTextSpeciality(String selectTextSpeciality) {
        this.selectTextSpeciality = selectTextSpeciality;
    }

    public String getSelectTextConsultant() {
        return selectTextConsultant;
    }

    public void setSelectTextConsultant(String selectTextConsultant) {
        this.selectTextConsultant = selectTextConsultant;
    }

    public String getSelectTextSession() {
        return selectTextSession;
    }

    public void setSelectTextSession(String selectTextSession) {
        this.selectTextSession = selectTextSession;
    }

    public ArrivalRecord getArrivalRecord() {
        return arrivalRecord;
    }

    public void setArrivalRecord(ArrivalRecord arrivalRecord) {
        this.arrivalRecord = arrivalRecord;
    }

    public FingerPrintRecordFacade getFpFacade() {
        return fpFacade;
    }

    public ServiceSessionLeaveController getServiceSessionLeaveController() {
        return serviceSessionLeaveController;
    }

    public ChannelBillController getChannelBillController() {
        return channelBillController;
    }

    public DoctorSpecialityController getDoctorSpecialityController() {
        return doctorSpecialityController;
    }

    public ChannelStaffPaymentBillController getChannelStaffPaymentBillController() {
        return channelStaffPaymentBillController;
    }

    public PaymentMethod getCanPayMetTmp() {
        return canPayMetTmp;
    }

    public void setCanPayMetTmp(PaymentMethod canPayMetTmp) {
        this.canPayMetTmp = canPayMetTmp;
    }

    public double getAbsentPercentage() {
        return absentPercentage;
    }

    public void setAbsentPercentage(double absentPercentage) {
        this.absentPercentage = absentPercentage;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = commonFunctions.getFirstDayOfWeek(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = commonFunctions.getLastDayOfWeek(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public double getStaffFaee() {
        return staffFaee;
    }

    public void setStaffFaee(double staffFaee) {
        this.staffFaee = staffFaee;
    }

    public double getHospitalFee() {
        return hospitalFee;
    }

    public void setHospitalFee(double hospitalFee) {
        this.hospitalFee = hospitalFee;
    }

    public double getScanFee() {
        return scanFee;
    }

    public void setScanFee(double scanFee) {
        this.scanFee = scanFee;
    }

    public double getVat() {
        return vat;
    }

    public void setVat(double vat) {
        this.vat = vat;
    }

    public double getTotalFee() {
        return totalFee;
    }

    public void setTotalFee(double totalFee) {
        this.totalFee = totalFee;
    }

    public String getFromDateStr() {
        if (getFromDate() == null) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
        String dateStr = sdf.format(getFromDate());
        return dateStr;
    }

    public String getToDateStr() {
        if (getFromDate() == null) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
        String dateStr = sdf.format(getToDate());
        return dateStr;
    }

    public FinalVariables getFinalVariables() {
        return finalVariables;
    }

    public void setFinalVariables(FinalVariables finalVariables) {
        this.finalVariables = finalVariables;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public SmsController getSmsController() {
        return smsController;
    }

    public void setSmsController(SmsController smsController) {
        this.smsController = smsController;
    }

    public List<ChannelSummeryRow> getChannelSummeryRows() {
        return channelSummeryRows;
    }

    public void setChannelSummeryRows(List<ChannelSummeryRow> channelSummeryRows) {
        this.channelSummeryRows = channelSummeryRows;
    }

    public AgentHistoryFacade getAgentHistoryFacade() {
        return agentHistoryFacade;
    }

    public void setAgentHistoryFacade(AgentHistoryFacade agentHistoryFacade) {
        this.agentHistoryFacade = agentHistoryFacade;
    }

    public PaymentFacade getPaymentFacade() {
        return paymentFacade;
    }

    public void setPaymentFacade(PaymentFacade paymentFacade) {
        this.paymentFacade = paymentFacade;
    }

    public BillComponentFacade getBillComponentFacade() {
        return billComponentFacade;
    }

    public void setBillComponentFacade(BillComponentFacade billComponentFacade) {
        this.billComponentFacade = billComponentFacade;
    }

    public PatientEncounterFacade getPatientEncounterFacade() {
        return patientEncounterFacade;
    }

    public void setPatientEncounterFacade(PatientEncounterFacade patientEncounterFacade) {
        this.patientEncounterFacade = patientEncounterFacade;
    }

}
