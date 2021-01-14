/**
 *
 */
package mz.org.fgh.mentoring.core.session.dao;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.stereotype.Repository;

import mz.co.mozview.frameworks.core.dao.GenericDAOImpl;
import mz.co.mozview.frameworks.core.dao.ParamBuilder;
import mz.co.mozview.frameworks.core.util.LifeCycleStatus;
import mz.org.fgh.mentoring.core.form.model.Form;
import mz.org.fgh.mentoring.core.location.model.Cabinet;
import mz.org.fgh.mentoring.core.location.model.District;
import mz.org.fgh.mentoring.core.location.model.HealthFacility;
import mz.org.fgh.mentoring.core.mentorship.model.Mentorship;
import mz.org.fgh.mentoring.core.programmaticarea.model.ProgrammaticArea;
import mz.org.fgh.mentoring.core.session.model.PerformedSession;
import mz.org.fgh.mentoring.core.session.model.Session;
import mz.org.fgh.mentoring.core.session.model.SubmitedSessions;
import mz.org.fgh.mentoring.core.tutor.model.Tutor;

/**
 * @author Stélio Moiane
 *
 */
@Repository(SessionDAO.NAME)
public class SessionDAOImpl extends GenericDAOImpl<Session, Long> implements SessionDAO {
	
	private static final String DATE_PATTERN = "yyyy-MM-dd";

	@Override
	public List<PerformedSession> findBySelectedFilter(final District district, final HealthFacility healthFacility,
	        final ProgrammaticArea programmaticArea, final Form form, final Tutor tutor, final Cabinet cabinet,
	        final LocalDate startDate, final LocalDate endDate, final LifeCycleStatus lifeCycleStatus) {

		final CriteriaBuilder criteriaBuilder = this.getEntityManager().getCriteriaBuilder();
		final CriteriaQuery<PerformedSession> createQuery = criteriaBuilder.createQuery(PerformedSession.class);
		final Root<Session> root = createQuery.from(Session.class);
		final Join<Session, Mentorship> mentorships = root.join("mentorships");

		mentorships.join("form").join("programmaticArea");
		mentorships.join("healthFacility").join("district");
		mentorships.join("tutor");
		final Join<Mentorship, Cabinet> cabinets = mentorships.join("cabinet", JoinType.LEFT);

		createQuery.select(criteriaBuilder.construct(PerformedSession.class, mentorships.get("form").get("name"),
		        criteriaBuilder.countDistinct(root.get("id"))));

		final List<Predicate> predicates = new ArrayList<>();

		if (district != null) {
			predicates.add(criteriaBuilder.equal(mentorships.get("healthFacility").get("district").get("uuid"),
			        district.getUuid()));
		}

		if (healthFacility != null) {
			predicates.add(
			        criteriaBuilder.equal(mentorships.get("healthFacility").get("uuid"), healthFacility.getUuid()));
		}

		if (programmaticArea != null) {
			predicates.add(criteriaBuilder.equal(mentorships.get("form").get("programmaticArea").get("uuid"),
			        programmaticArea.getUuid()));
		}

		if (form != null) {
			predicates.add(criteriaBuilder.equal(mentorships.get("form").get("uuid"), form.getUuid()));
		}

		if (tutor != null) {
			predicates.add(criteriaBuilder.equal(mentorships.get("tutor").get("uuid"), tutor.getUuid()));
		}

		if (cabinet != null) {
			predicates.add(criteriaBuilder.equal(cabinets.get("uuid"), cabinet.getUuid()));
		}

		if (startDate != null) {
			predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("performedDate"), startDate));
		}

		if (endDate != null) {
			predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("performedDate"), endDate));
		}

		predicates.add(criteriaBuilder.equal(root.get("lifeCycleStatus"), lifeCycleStatus));

		createQuery.where(predicates.toArray(new Predicate[predicates.size()]));

		createQuery.groupBy(mentorships.get("form").get("id"));

		createQuery.orderBy(criteriaBuilder.asc(mentorships.get("form").get("name")));

		final TypedQuery<PerformedSession> query = this.getEntityManager().createQuery(createQuery);

		return query.getResultList();
	}

	@Override
	public List<SubmitedSessions> findNumberOfSessionsPerDistrict(final LifeCycleStatus lifeCycleStatus) {
		return this.findByNamedQuery(SessionDAO.QUERY_NAME.findNumberOfSessionsPerDistrict,
		        new ParamBuilder().add("lifeCycleStatus", lifeCycleStatus).process(), SubmitedSessions.class);
	}

	@Override
	public List<PerformedSession> findByTutorAndForm(final Tutor tutor, final Form form, final LocalDate startDate,
	        final LocalDate endDate) {

		final CriteriaBuilder criteriaBuilder = this.getEntityManager().getCriteriaBuilder();

		final CriteriaQuery<PerformedSession> createQuery = criteriaBuilder.createQuery(PerformedSession.class);

		final Root<Session> root = createQuery.from(Session.class);

		final Join<Session, Mentorship> mentorships = root.join("mentorships");

		mentorships.join("tutor");
		mentorships.join("form");
		mentorships.join("healthFacility").join("district");

		createQuery.select(criteriaBuilder.construct(PerformedSession.class,
		        mentorships.get("healthFacility").get("district").get("district"),
		        mentorships.get("healthFacility").get("healthFacility"),
		        criteriaBuilder.countDistinct(root.get("id"))));

		final List<Predicate> predicates = new ArrayList<>();

		if (tutor != null) {
			predicates.add(criteriaBuilder.equal(mentorships.get("tutor").get("uuid"), tutor.getUuid()));
		}

		if (form != null) {
			predicates.add(criteriaBuilder.equal(mentorships.get("form").get("uuid"), form.getUuid()));
		}

		if (startDate != null) {
			predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("performedDate"), startDate));
		}

		if (endDate != null) {
			predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("performedDate"), endDate));
		}

		predicates.add(criteriaBuilder.equal(root.get("lifeCycleStatus"), LifeCycleStatus.ACTIVE));

		createQuery.where(predicates.toArray(new Predicate[predicates.size()]));

		createQuery.groupBy(mentorships.get("healthFacility").get("district").get("district"),
		        mentorships.get("healthFacility").get("healthFacility"));

		createQuery.orderBy(criteriaBuilder.asc(mentorships.get("healthFacility").get("district").get("district")),
		        criteriaBuilder.asc(mentorships.get("healthFacility").get("healthFacility")));

		final TypedQuery<PerformedSession> query = this.getEntityManager().createQuery(createQuery);

		return query.getResultList();
	}

	@Override
	public List<Session> findWithDuplicatedUuids(final LifeCycleStatus lifeCycleStatus) {
		return this.findByNamedQuery(SessionDAO.QUERY_NAME.findWithDuplicatedUuids,
		        new ParamBuilder().add("lifeCycleStatus", lifeCycleStatus).process());
	}

	@Override
	public List<Session> fetchSessionsByUuid(final String sessionUuid, final LifeCycleStatus lifeCycleStatus) {
		return this.findByNamedQuery(SessionDAO.QUERY_NAME.fetchSessionsByUuid,
		        new ParamBuilder().add("sessionUuid", sessionUuid).add("lifeCycleStatus", lifeCycleStatus).process());
	}

	@Override
	public List<PerformedSession> findBySelectedFilterList(final District district, final HealthFacility healthFacility,
	        final ProgrammaticArea programmaticArea, final Form form, final Tutor tutor, final Cabinet cabinet,
	        final LocalDate startDate, final LocalDate endDate, final LifeCycleStatus lifeCycleStatus) {

		final CriteriaBuilder criteriaBuilder = this.getEntityManager().getCriteriaBuilder();
		final CriteriaQuery<PerformedSession> createQuery = criteriaBuilder.createQuery(PerformedSession.class);
		final Root<Session> root = createQuery.from(Session.class);
		final Join<Session, Mentorship> mentorships = root.join("mentorships");

		mentorships.join("form").join("programmaticArea");
		mentorships.join("healthFacility").join("district");
		mentorships.join("tutor").join("career");
		final Join<Mentorship, Cabinet> cabinets = mentorships.join("cabinet", JoinType.LEFT);

		createQuery.select(criteriaBuilder.construct(PerformedSession.class, mentorships.get("form").get("name"),
		        root.get("createdAt"), root.get("performedDate"),
		        mentorships.get("healthFacility").get("district").get("district"),
		        mentorships.get("healthFacility").get("healthFacility"), cabinets.get("name"),
		        mentorships.get("tutor").get("name"), mentorships.get("tutor").get("surname"),
		        mentorships.get("tutor").get("career").get("position"), root.get("startDate"), root.get("endDate"),
		        root.get("status")));

		final List<Predicate> predicates = new ArrayList<>();

		if (district != null) {
			predicates.add(criteriaBuilder.equal(mentorships.get("healthFacility").get("district").get("uuid"),
			        district.getUuid()));
		}

		if (healthFacility != null) {
			predicates.add(
			        criteriaBuilder.equal(mentorships.get("healthFacility").get("uuid"), healthFacility.getUuid()));
		}

		if (programmaticArea != null) {
			predicates.add(criteriaBuilder.equal(mentorships.get("form").get("programmaticArea").get("uuid"),
			        programmaticArea.getUuid()));
		}

		if (form != null) {
			predicates.add(criteriaBuilder.equal(mentorships.get("form").get("uuid"), form.getUuid()));
		}

		if (tutor != null) {
			predicates.add(criteriaBuilder.equal(mentorships.get("tutor").get("uuid"), tutor.getUuid()));
		}

		if (cabinet != null) {
			predicates.add(criteriaBuilder.equal(cabinets.get("uuid"), cabinet.getUuid()));
		}

		if (startDate != null) {
			predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("performedDate"), startDate));
		}

		if (endDate != null) {
			predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("performedDate"), endDate));
		}

		predicates.add(criteriaBuilder.equal(root.get("lifeCycleStatus"), lifeCycleStatus));

		createQuery.where(predicates.toArray(new Predicate[predicates.size()]));

		createQuery.groupBy(root.get("id"));

		createQuery.orderBy(criteriaBuilder.asc(mentorships.get("form").get("name")),
		        criteriaBuilder.asc(root.get("performedDate")),
		        criteriaBuilder.asc(mentorships.get("healthFacility").get("district").get("district")));

		final TypedQuery<PerformedSession> query = this.getEntityManager().createQuery(createQuery);

		return query.getResultList();
	}

	/**
	 *
	 */
	@SuppressWarnings({ "unchecked" })
	@Override
	public List<PerformedSession> findBySelectedFilterHTS(LocalDate startDate,LocalDate endDate) {

		final StringBuilder nativeQuery = new StringBuilder();
		nativeQuery.append("SELECT d.DISTRICT AS 'districtName', h.HEALTH_FACILITY AS 'healthFacility', DATE_FORMAT(s.PERFORMED_DATE,'%d-%m-%Y') AS 'performedDate', CONCAT(t.NAME,' ',t.SURNAME) AS 'tutorName', CONCAT(tr.NAME,' ',tr.SURNAME) AS 'tutoredName', c.NAME AS 'cabinet', CASE m.DOOR WHEN 'P1' THEN '1' WHEN 'P2' THEN '2' WHEN 'P3' THEN '3' WHEN 'P4' THEN '4' END AS 'door', CASE m.TIME_OF_DAY WHEN 'DAY' THEN 'Dia' WHEN 'LATE_NIGHT' THEN 'Tarde/Noite' ELSE '' END AS 'timeOfDay', atendidos.NUMERIC_VALUE AS 'atendidos', previos.NUMERIC_VALUE AS 'previos', testados.NUMERIC_VALUE AS  'testados', positivos.NUMERIC_VALUE AS 'positivos', inscritos.NUMERIC_VALUE AS 'inscritos',DATE_FORMAT(s.CREATED_AT,'%d-%m-%Y %H:%i:%s') AS 'createdAt' FROM SESSIONS s INNER JOIN MENTORSHIPS m ON s.ID=m.SESSION_ID INNER JOIN HEALTH_FACILITIES h ON m.HEALTH_FACILITY_ID=h.ID INNER JOIN TUTORS t ON m.TUTOR_ID=t.ID INNER JOIN TUTOREDS tr ON m.TUTORED_ID=tr.ID INNER JOIN CABINETS c ON m.CABINET_ID=c.ID INNER JOIN FORMS f ON m.FORM_ID=f.ID INNER JOIN DISTRICTS d ON h.DISTRICT_ID = d.ID LEFT JOIN (SELECT MENTORSHIP_ID, NUMERIC_VALUE FROM ANSWERS a,QUESTIONS q WHERE a.QUESTION_ID=q.ID AND q.CODE='MTQ00000588') atendidos ON atendidos.MENTORSHIP_ID=m.ID LEFT JOIN (SELECT MENTORSHIP_ID, NUMERIC_VALUE FROM ANSWERS a,QUESTIONS q WHERE a.QUESTION_ID=q.ID AND q.CODE='MTQ00000589') previos ON previos.MENTORSHIP_ID=m.ID LEFT JOIN (SELECT MENTORSHIP_ID, NUMERIC_VALUE FROM ANSWERS a,QUESTIONS q WHERE a.QUESTION_ID=q.ID AND q.CODE='MTQ00000590') testados ON testados.MENTORSHIP_ID=m.ID LEFT JOIN (SELECT MENTORSHIP_ID, NUMERIC_VALUE FROM ANSWERS a,QUESTIONS q WHERE a.QUESTION_ID=q.ID AND q.CODE='MTQ00000591') positivos ON positivos.MENTORSHIP_ID=m.ID LEFT JOIN (SELECT MENTORSHIP_ID, NUMERIC_VALUE FROM ANSWERS a,QUESTIONS q WHERE a.QUESTION_ID=q.ID AND q.CODE='MTQ00000592') inscritos ON inscritos.MENTORSHIP_ID=m.ID  WHERE s.LIFE_CYCLE_STATUS='ACTIVE' AND m.LIFE_CYCLE_STATUS='ACTIVE' AND s.PERFORMED_DATE>='"+startDate.format(DateTimeFormatter.ofPattern(DATE_PATTERN))+"' AND s.PERFORMED_DATE<='"+endDate.format(DateTimeFormatter.ofPattern(DATE_PATTERN))+"' AND f.CODE='MT00000045' ORDER BY 1,2,3 ASC");

		final Query query = this.getEntityManager().createNativeQuery(nativeQuery.toString());
		
		List<Object[]> performedSessionsHTS = query.getResultList();
		 
		List<PerformedSession> performedSessions= new ArrayList<PerformedSession>(0);
		
		for (Object[] ps : performedSessionsHTS) {
			PerformedSession p= new PerformedSession(
					ps[0].toString(), 
					ps[1].toString(), 
					ps[2].toString(), 
					ps[3].toString(), 
					ps[4].toString(), 
					ps[5].toString(), 
					ps[6].toString(), 
					ps[7].toString(), 
					Long.valueOf(ps[8].toString()),
					Long.valueOf(ps[9].toString()), 
					Long.valueOf(ps[10].toString()), 
					Long.valueOf(ps[11].toString()), 
					Long.valueOf(ps[12].toString()), 
					ps[13].toString());
			
			performedSessions.add(p);
			
		}

		return performedSessions;
		
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<PerformedSession> findBySelectedFilterNarrative(LocalDate startDate, LocalDate endDate) {
		
		final StringBuilder nativeQuery = new StringBuilder();
		
		String start=startDate.format(DateTimeFormatter.ofPattern(DATE_PATTERN));
		String end=endDate.format(DateTimeFormatter.ofPattern(DATE_PATTERN));
		
		nativeQuery.append("SELECT d.PROVINCE,  (SELECT COUNT(DISTINCT(m.SESSION_ID)) FROM MENTORSHIPS m, FORMS f WHERE m.FORM_ID=f.ID AND m.LIFE_CYCLE_STATUS='ACTIVE' AND f.CODE='MT00000034' AND m.PERFORMED_DATE>='" + start + "' AND m.PERFORMED_DATE<='" + end + "') AS 'preventionVCT', (SELECT COUNT(DISTINCT(m.SESSION_ID)) FROM MENTORSHIPS m, FORMS f WHERE m.FORM_ID=f.ID AND m.LIFE_CYCLE_STATUS='ACTIVE' AND f.CODE='MT00000033' AND m.PERFORMED_DATE>='" + start + "' AND m.PERFORMED_DATE<='" + end + "') AS 'preventionPICT', (SELECT COUNT(DISTINCT(m.SESSION_ID)) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID=f.ID AND a.QUESTION_ID=q.ID AND m.LIFE_CYCLE_STATUS='ACTIVE' AND m.ID=a.MENTORSHIP_ID AND q.CODE='MTQ00000290' AND f.CODE IN ('MT00000033','MT00000034') AND m.PERFORMED_DATE>='" + start + "' AND m.PERFORMED_DATE<='" + end + "' AND EXISTS ( SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID=m.ID AND a2.TEXT_VALUE IN ('COMPETENTE','NAO SATISFATORIO'))) AS 'preventionIndexCase', (SELECT COUNT(DISTINCT(m.SESSION_ID)) FROM MENTORSHIPS m, FORMS f, CABINETS c WHERE m.FORM_ID=f.ID AND m.CABINET_ID=c.ID AND m.LIFE_CYCLE_STATUS='ACTIVE' AND c.ID IN (12) AND f.CODE IN ('MT00000001','MT00000033') AND m.PERFORMED_DATE>='" + start + "' AND m.PERFORMED_DATE<='" + end + "') AS 'preventionSaaj', (SELECT COUNT(DISTINCT(m.SESSION_ID)) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID=f.ID AND a.QUESTION_ID=q.ID AND m.LIFE_CYCLE_STATUS='ACTIVE' AND m.ID=a.MENTORSHIP_ID AND q.CODE IN ('MTQ00000291','MTQ00000292','MTQ00000293') AND f.CODE IN ('MT00000033','MT00000034') AND m.PERFORMED_DATE>='" + start + "' AND m.PERFORMED_DATE<='" + end + "' AND EXISTS ( SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID=m.ID AND a2.TEXT_VALUE IN ('COMPETENTE','NAO SATISFATORIO'))) AS 'preventionHtcLink', (SELECT COUNT(DISTINCT(m.SESSION_ID)) FROM MENTORSHIPS m, FORMS f WHERE m.FORM_ID=f.ID AND m.LIFE_CYCLE_STATUS='ACTIVE' AND f.CODE IN ('MT00000036','MT00000038','MT00000039') AND m.PERFORMED_DATE>='" + start + "' AND m.PERFORMED_DATE<='" + end + "') AS 'preventionANC',   (SELECT COUNT(DISTINCT(m.SESSION_ID)) FROM MENTORSHIPS m, FORMS f, CABINETS c WHERE m.FORM_ID=f.ID AND m.CABINET_ID=c.ID AND m.LIFE_CYCLE_STATUS='ACTIVE' AND c.ID IN (4) AND f.CODE IN ('MT00000001','MT00000033') AND m.PERFORMED_DATE>='" + start + "' AND m.PERFORMED_DATE<='" + end + "') AS 'preventionCPN',   (SELECT COUNT(DISTINCT(m.SESSION_ID)) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID=f.ID AND a.QUESTION_ID=q.ID AND m.LIFE_CYCLE_STATUS='ACTIVE' AND m.ID=a.MENTORSHIP_ID AND q.CODE IN ('MTQ00000312','MTQ00000313','MTQ00000314') AND f.CODE IN ('MT00000035') AND m.PERFORMED_DATE>='" + start + "' AND m.PERFORMED_DATE<='" + end + "' AND EXISTS ( SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID=m.ID AND a2.TEXT_VALUE IN ('COMPETENTE','NAO SATISFATORIO')) AND m.HEALTH_FACILITY_ID=128) AS 'ctStiAdultsPrison', (SELECT COUNT(DISTINCT(m.SESSION_ID)) FROM MENTORSHIPS m, FORMS f WHERE m.FORM_ID=f.ID AND m.LIFE_CYCLE_STATUS='ACTIVE' AND f.CODE IN ('MT00000035') AND m.PERFORMED_DATE>='" + start + "' AND m.PERFORMED_DATE<='" + end + "' AND m.HEALTH_FACILITY_ID=128) AS 'ctAdultsPrison', (SELECT COUNT(DISTINCT(m.SESSION_ID)) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID=f.ID AND a.QUESTION_ID=q.ID AND m.LIFE_CYCLE_STATUS='ACTIVE' AND m.ID=a.MENTORSHIP_ID AND q.CODE IN ('MTQ00000317','MTQ00000318','MTQ00000319','MTQ00000521') AND f.CODE IN ('MT00000035','MT00000036','MT00000037','MT00000038','MT00000039','MT00000043') AND m.HEALTH_FACILITY_ID=128 AND m.PERFORMED_DATE>='" + start + "' AND m.PERFORMED_DATE<='" + end + "' AND EXISTS ( SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID=m.ID AND a2.TEXT_VALUE IN ('COMPETENTE','NAO SATISFATORIO'))) AS 'ctAdultsVLPrison', (SELECT COUNT(DISTINCT(m.SESSION_ID)) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID=f.ID AND a.QUESTION_ID=q.ID AND m.LIFE_CYCLE_STATUS='ACTIVE' AND m.ID=a.MENTORSHIP_ID AND q.CODE IN ('MTQ00000307','MTQ00000308','MTQ00000309','MTQ00000310','MTQ00000311','MTQ00000376') AND f.CODE IN ('MT00000035','MT00000037') AND m.PERFORMED_DATE>='" + start + "' AND m.PERFORMED_DATE<='" + end + "' AND EXISTS ( SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID=m.ID AND a2.TEXT_VALUE IN ('COMPETENTE','NAO SATISFATORIO')) AND m.HEALTH_FACILITY_ID=128) AS 'ctTbHivPrison', (SELECT COUNT(DISTINCT(m.SESSION_ID)) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID=f.ID AND a.QUESTION_ID=q.ID AND m.LIFE_CYCLE_STATUS='ACTIVE' AND m.ID=a.MENTORSHIP_ID AND q.CODE IN ('MTQ00000417','MTQ00000418','MTQ00000419','MTQ00000420','MTQ00000421','MTQ00000422','MTQ00000423','MTQ00000424','MTQ00000425','MTQ00000426','MTQ00000427','MTQ00000428','MTQ00000429','MTQ00000430','MTQ00000431','MTQ00000432','MTQ00000433') AND f.CODE IN ('MT00000040') AND m.PERFORMED_DATE>='" + start + "' AND m.PERFORMED_DATE<='" + end + "' AND EXISTS ( SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID=m.ID AND a2.TEXT_VALUE IN ('COMPETENTE','NAO SATISFATORIO'))) AS 'ctApss', (SELECT COUNT(DISTINCT(m.SESSION_ID)) FROM MENTORSHIPS m, FORMS f WHERE m.FORM_ID=f.ID AND m.LIFE_CYCLE_STATUS='ACTIVE' AND f.CODE IN ('MT00000035') AND m.PERFORMED_DATE>='" + start + "' AND m.PERFORMED_DATE<='" + end + "') AS 'ctAdults', (SELECT COUNT(DISTINCT(m.SESSION_ID)) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID=f.ID AND a.QUESTION_ID=q.ID AND m.LIFE_CYCLE_STATUS='ACTIVE' AND m.ID=a.MENTORSHIP_ID AND q.CODE IN ('MTQ00000317','MTQ00000318','MTQ00000319') AND f.CODE='MT00000035' AND m.PERFORMED_DATE>='" + start + "' AND m.PERFORMED_DATE<='" + end + "' AND EXISTS ( SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID=m.ID AND a2.TEXT_VALUE IN ('COMPETENTE','NAO SATISFATORIO'))) AS 'ctAdultsVL', (SELECT COUNT(DISTINCT(m.SESSION_ID)) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID=f.ID AND a.QUESTION_ID=q.ID AND m.LIFE_CYCLE_STATUS='ACTIVE' AND m.ID=a.MENTORSHIP_ID AND q.CODE IN ('MTQ00000328') AND f.CODE IN ('MT00000035') AND m.PERFORMED_DATE>='" + start + "' AND m.PERFORMED_DATE<='" + end + "' AND EXISTS ( SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID=m.ID AND a2.TEXT_VALUE IN ('COMPETENTE','NAO SATISFATORIO'))) AS 'ctInh',   (SELECT COUNT(DISTINCT(m.SESSION_ID)) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID=f.ID AND a.QUESTION_ID=q.ID AND m.LIFE_CYCLE_STATUS='ACTIVE' AND m.ID=a.MENTORSHIP_ID AND q.CODE IN ('MTQ00000307','MTQ00000308','MTQ00000309','MTQ00000310','MTQ00000311','MTQ00000376') AND f.CODE IN ('MT00000035','MT00000037') AND m.PERFORMED_DATE>='" + start + "' AND m.PERFORMED_DATE<='" + end + "' AND EXISTS ( SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID=m.ID AND a2.TEXT_VALUE IN ('COMPETENTE','NAO SATISFATORIO'))) AS 'ctTbHiv',    (SELECT COUNT(DISTINCT(m.SESSION_ID)) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID=f.ID AND a.QUESTION_ID=q.ID AND m.LIFE_CYCLE_STATUS='ACTIVE' AND m.ID=a.MENTORSHIP_ID AND q.CODE IN ('MTQ00000305','MTQ00000306') AND f.CODE IN ('MT00000035') AND m.PERFORMED_DATE>='" + start + "' AND m.PERFORMED_DATE<='" + end + "' AND a.TEXT_VALUE IN ('COMPETENTE','NAO SATISFATORIO')) AS 'ctNutrition', (SELECT COUNT(DISTINCT(m.TUTORED_ID)) FROM MENTORSHIPS m, FORMS f WHERE m.FORM_ID=f.ID AND m.LIFE_CYCLE_STATUS='ACTIVE' AND f.CODE='MT00000040' AND m.PERFORMED_DATE>='" + start + "' AND m.PERFORMED_DATE<='" + end + "') AS 'ctApssTutoreds', (SELECT COUNT(DISTINCT(m.SESSION_ID)) FROM MENTORSHIPS m, FORMS f WHERE m.FORM_ID=f.ID AND m.LIFE_CYCLE_STATUS='ACTIVE' AND f.CODE='MT00000040' AND m.PERFORMED_DATE>='" + start + "' AND m.PERFORMED_DATE<='" + end + "') AS 'ctApssSessions', (SELECT COUNT(DISTINCT(m.SESSION_ID)) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID=f.ID AND a.QUESTION_ID=q.ID AND m.LIFE_CYCLE_STATUS='ACTIVE' AND m.ID=a.MENTORSHIP_ID AND q.CODE IN ('MTQ00000332','MTQ00000333','MTQ00000361','MTQ00000527') AND f.CODE IN ('MT00000035','MT00000043','MT00000037') AND m.PERFORMED_DATE>='" + start + "' AND m.PERFORMED_DATE<='" + end + "' AND EXISTS ( SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID=m.ID AND a2.TEXT_VALUE IN ('COMPETENTE','NAO SATISFATORIO'))) AS 'ctEAC',  (SELECT COUNT(DISTINCT(m.SESSION_ID)) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID=f.ID AND a.QUESTION_ID=q.ID AND m.LIFE_CYCLE_STATUS='ACTIVE' AND m.ID=a.MENTORSHIP_ID AND q.CODE IN ('MTQ00000332','MTQ00000333','MTQ00000361','MTQ00000527') AND f.CODE IN ('MT00000035','MT00000037','MT00000043') AND m.PERFORMED_DATE>='" + start + "' AND m.PERFORMED_DATE<='" + end + "' AND EXISTS ( SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID=m.ID AND a2.TEXT_VALUE IN ('COMPETENTE','NAO SATISFATORIO'))) AS 'ctMDC',  (SELECT COUNT(DISTINCT(m.SESSION_ID)) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID=f.ID AND a.QUESTION_ID=q.ID AND m.LIFE_CYCLE_STATUS='ACTIVE' AND m.ID=a.MENTORSHIP_ID AND q.CODE='MTQ00000400' AND f.CODE='MT00000038' AND m.PERFORMED_DATE>='" + start + "' AND m.PERFORMED_DATE<='" + end + "' AND EXISTS ( SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID=m.ID AND a2.TEXT_VALUE IN ('COMPETENTE','NAO SATISFATORIO'))) AS 'ctCervical',  (SELECT COUNT(DISTINCT(m.SESSION_ID)) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID=f.ID AND a.QUESTION_ID=q.ID AND m.LIFE_CYCLE_STATUS='ACTIVE' AND m.ID=a.MENTORSHIP_ID AND q.CODE IN ('MTQ00000312','MTQ00000313','MTQ00000314') AND f.CODE IN ('MT00000035') AND m.PERFORMED_DATE>='" + start + "' AND m.PERFORMED_DATE<='" + end + "' AND EXISTS ( SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID=m.ID AND a2.TEXT_VALUE IN ('COMPETENTE','NAO SATISFATORIO'))) AS 'ctStiAdults',   (SELECT COUNT(DISTINCT(m.SESSION_ID)) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID=f.ID AND a.QUESTION_ID=q.ID AND m.LIFE_CYCLE_STATUS='ACTIVE' AND m.ID=a.MENTORSHIP_ID AND q.CODE IN('MTQ00000377','MTQ00000307','MTQ00000308','MTQ00000309','MTQ00000376') AND f.CODE IN ('MT00000035','MT00000039','MT00000037') AND m.PERFORMED_DATE>='" + start + "' AND m.PERFORMED_DATE<='" + end + "' AND EXISTS ( SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID=m.ID AND a2.TEXT_VALUE IN ('COMPETENTE','NAO SATISFATORIO'))) AS 'tbSessions', (SELECT COUNT(DISTINCT(m.SESSION_ID)) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID=f.ID AND a.QUESTION_ID=q.ID AND m.LIFE_CYCLE_STATUS='ACTIVE' AND m.ID=a.MENTORSHIP_ID AND q.CODE IN('MTQ00000310','MTQ00000311','MTQ00000308','MTQ00000309') AND f.CODE IN ('MT00000035','MT00000037') AND m.PERFORMED_DATE>='" + start + "' AND m.PERFORMED_DATE<='" + end + "' AND EXISTS ( SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID=m.ID AND a2.TEXT_VALUE IN ('COMPETENTE','NAO SATISFATORIO'))) AS 'tbSessionsCt', (SELECT COUNT(DISTINCT(m.SESSION_ID)) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID=f.ID AND a.QUESTION_ID=q.ID AND m.LIFE_CYCLE_STATUS='ACTIVE' AND m.ID=a.MENTORSHIP_ID AND q.CODE IN('MTQ00000403','MTQ00000406','MTQ00000356') AND f.CODE='MT00000039' AND m.PERFORMED_DATE>='" + start + "' AND m.PERFORMED_DATE<='" + end + "' AND EXISTS ( SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID=m.ID AND a2.TEXT_VALUE IN ('COMPETENTE','NAO SATISFATORIO'))) AS 'tbInh', (SELECT COUNT(DISTINCT(m.SESSION_ID)) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID=f.ID AND a.QUESTION_ID=q.ID AND m.LIFE_CYCLE_STATUS='ACTIVE' AND m.ID=a.MENTORSHIP_ID AND q.CODE IN('MTQ00000310','MTQ00000311','MTQ00000308','MTQ00000309','MTQ00000376') AND f.CODE='MT00000037' AND m.PERFORMED_DATE>='" + start + "' AND m.PERFORMED_DATE<='" + end + "' AND EXISTS ( SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID=m.ID AND a2.TEXT_VALUE IN ('COMPETENTE','NAO SATISFATORIO'))) AS 'tbSessionsPediatric', (SELECT COUNT(DISTINCT(m.SESSION_ID)) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID=f.ID AND a.QUESTION_ID=q.ID AND m.LIFE_CYCLE_STATUS='ACTIVE' AND m.ID=a.MENTORSHIP_ID AND q.CODE IN('MTQ00000305','MTQ00000389','MTQ00000306','MTQ00000390','MTQ00000391') AND f.CODE='MT00000037' AND m.PERFORMED_DATE>='" + start + "' AND m.PERFORMED_DATE<='" + end + "' AND EXISTS ( SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID=m.ID AND a2.TEXT_VALUE IN ('COMPETENTE','NAO SATISFATORIO'))) AS 'pediatricNutrition', (SELECT COUNT(DISTINCT(m.SESSION_ID)) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID=f.ID AND a.QUESTION_ID=q.ID AND m.LIFE_CYCLE_STATUS='ACTIVE' AND m.ID=a.MENTORSHIP_ID AND q.CODE IN('MTQ00000329','MTQ00000392','MTQ00000393') AND f.CODE='MT00000037' AND m.PERFORMED_DATE>='" + start + "' AND m.PERFORMED_DATE<='" + end + "' AND EXISTS ( SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID=m.ID AND a2.TEXT_VALUE IN ('COMPETENTE','NAO SATISFATORIO'))) AS 'pediatricStarART', (SELECT COUNT(DISTINCT(m.SESSION_ID)) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID=f.ID AND a.QUESTION_ID=q.ID AND m.LIFE_CYCLE_STATUS='ACTIVE' AND m.ID=a.MENTORSHIP_ID AND q.CODE IN('MTQ00000330','MTQ00000331','MTQ00000332') AND f.CODE='MT00000037' AND m.PERFORMED_DATE>='" + start + "' AND m.PERFORMED_DATE<='" + end + "' AND EXISTS ( SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID=m.ID AND a2.TEXT_VALUE IN ('COMPETENTE','NAO SATISFATORIO'))) AS 'pediatricAMA', (SELECT COUNT(DISTINCT(m.SESSION_ID)) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID=f.ID AND a.QUESTION_ID=q.ID AND m.LIFE_CYCLE_STATUS='ACTIVE' AND m.ID=a.MENTORSHIP_ID AND q.CODE IN('MTQ00000308','MTQ00000309','MTQ00000310') AND f.CODE='MT00000037' AND m.PERFORMED_DATE>='" + start + "' AND m.PERFORMED_DATE<='" + end + "' AND EXISTS ( SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID=m.ID AND a2.TEXT_VALUE IN ('COMPETENTE','NAO SATISFATORIO'))) AS 'pediatricTB', (SELECT COUNT(DISTINCT(m.SESSION_ID)) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID=f.ID AND a.QUESTION_ID=q.ID AND m.LIFE_CYCLE_STATUS='ACTIVE' AND m.ID=a.MENTORSHIP_ID AND q.CODE IN('MTQ00000317','MTQ00000318','MTQ00000319') AND f.CODE='MT00000037' AND m.PERFORMED_DATE>='" + start + "' AND m.PERFORMED_DATE<='" + end + "' AND EXISTS ( SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID=m.ID AND a2.TEXT_VALUE IN ('COMPETENTE','NAO SATISFATORIO'))) AS 'pediatricVL' FROM DISTRICTS d GROUP BY PROVINCE");

		final Query query = this.getEntityManager().createNativeQuery(nativeQuery.toString());
		
		List<Object[]> performedSessionsHTS = query.getResultList();
		 
		List<PerformedSession> performedSessions= new ArrayList<PerformedSession>(0);
		
		for (Object[] ps : performedSessionsHTS) {
			PerformedSession p= new PerformedSession(
					ps[0].toString(),
					Long.valueOf(ps[1].toString()), 
					Long.valueOf(ps[2].toString()),
					Long.valueOf(ps[3].toString()),
					Long.valueOf(ps[4].toString()),
					Long.valueOf(ps[5].toString()),
					Long.valueOf(ps[6].toString()),
					Long.valueOf(ps[7].toString()),
					Long.valueOf(ps[8].toString()),
					Long.valueOf(ps[9].toString()),
					Long.valueOf(ps[10].toString()),
					Long.valueOf(ps[11].toString()),
					Long.valueOf(ps[12].toString()),
					Long.valueOf(ps[13].toString()),
					Long.valueOf(ps[14].toString()),
					Long.valueOf(ps[15].toString()),
					Long.valueOf(ps[16].toString()),
					Long.valueOf(ps[17].toString()),
					Long.valueOf(ps[18].toString()),
					Long.valueOf(ps[19].toString()),
					Long.valueOf(ps[20].toString()),
					Long.valueOf(ps[21].toString()),
					Long.valueOf(ps[22].toString()),
					Long.valueOf(ps[23].toString()),
					Long.valueOf(ps[24].toString()),
					Long.valueOf(ps[25].toString()),
					Long.valueOf(ps[26].toString()),
					Long.valueOf(ps[27].toString()),
					Long.valueOf(ps[28].toString()),
					Long.valueOf(ps[29].toString()),
					Long.valueOf(ps[30].toString()),
					Long.valueOf(ps[31].toString()),
					Long.valueOf(ps[32].toString())
);
			
			performedSessions.add(p);
			
		}

		return performedSessions;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<PerformedSession> findBySelectedFilterLast12Months() {
		
final StringBuilder nativeQuery = new StringBuilder();
		
		nativeQuery.append("SELECT CONCAT( MONTHNAME(m.PERFORMED_DATE) ,' ',YEAR(m.PERFORMED_DATE)) AS 'monthName', CONCAT(YEAR(m.PERFORMED_DATE),'',DATE_FORMAT(m.PERFORMED_DATE,'%m')) AS 'month', COUNT(DISTINCT(SESSION_ID)) AS sessions FROM MENTORSHIPS m WHERE m.LIFE_CYCLE_STATUS='ACTIVE'  GROUP BY CONCAT( MONTH(m.PERFORMED_DATE) ,' ',YEAR(m.PERFORMED_DATE)) ORDER BY CONCAT(YEAR(m.PERFORMED_DATE),'', DATE_FORMAT(m.PERFORMED_DATE,'%m')) DESC LIMIT 12");

		final Query query = this.getEntityManager().createNativeQuery(nativeQuery.toString());
		
		List<Object[]> performedSessionsHTS = query.getResultList();
		 
		List<PerformedSession> performedSessions= new ArrayList<PerformedSession>(0);
		
		for (Object[] ps : performedSessionsHTS) {
			PerformedSession p= new PerformedSession(
					ps[0].toString(),
					ps[1].toString(), 
					Long.valueOf(ps[2].toString()));
			
			performedSessions.add(p);
			
		}

		return performedSessions;
		
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<PerformedSession> findByTutor(Tutor tutor, LocalDate startDate, LocalDate endDate) {
		
final StringBuilder nativeQuery = new StringBuilder();
		
		nativeQuery.append("SELECT f.NAME AS 'formName', COUNT(DISTINCT(m.SESSION_ID)) AS 'totalPerformed' FROM MENTORSHIPS m,FORMS f,TUTORS t WHERE m.PERFORMED_DATE>='"+startDate+"' AND m.PERFORMED_DATE<='"+endDate+"' AND m.FORM_ID=f.ID AND m.TUTOR_ID=t.ID AND t.uuid='"+tutor.getUuid()+"' AND m.LIFE_CYCLE_STATUS='ACTIVE' GROUP BY f.ID");

		final Query query = this.getEntityManager().createNativeQuery(nativeQuery.toString());
		
		List<Object[]> performedSessionsHTS = query.getResultList();
		 
		List<PerformedSession> performedSessions= new ArrayList<PerformedSession>(0);
		
		for (Object[] ps : performedSessionsHTS) {
			PerformedSession p= new PerformedSession(
					ps[0].toString(),
					Long.valueOf(ps[1].toString()));
			
			performedSessions.add(p);
			
		}

		return performedSessions;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<PerformedSession> findBySelectedFilterIndicators(LocalDate startDate, LocalDate endDate) {
		final StringBuilder nativeQuery = new StringBuilder();
		nativeQuery.append("SELECT f.NAME,COUNT(i.ID) AS 'total' FROM INDICATORS i, FORMS f WHERE i.FORM_ID=f.ID AND i.LIFE_CYCLE_STATUS='ACTIVE' AND i.PERFORMED_DATE >='"+startDate.format(DateTimeFormatter.ofPattern(DATE_PATTERN))+"' AND i.PERFORMED_DATE <='"+endDate.format(DateTimeFormatter.ofPattern(DATE_PATTERN))+"' AND f.NAME LIKE '%PROCEDIMENTO OPERACIONAL%' GROUP BY i.FORM_ID ORDER BY 1 ASC" );

		final Query query = this.getEntityManager().createNativeQuery(nativeQuery.toString());
		
		List<Object[]> performedSessionsHTS = query.getResultList();
		 
		List<PerformedSession> performedSessions= new ArrayList<PerformedSession>(0);
		
		for (Object[] ps : performedSessionsHTS) {
			PerformedSession p= new PerformedSession(
					ps[0].toString(), 
					Long.valueOf(ps[1].toString()));
			
			performedSessions.add(p);
			
		}

		return performedSessions;
	}
	
	@SuppressWarnings({ "unchecked" })
	@Override
	public List<PerformedSession> findBySelectedFilterIndicatorsList(LocalDate startDate,LocalDate endDate) {

		final StringBuilder nativeQuery = new StringBuilder();
		nativeQuery.append("SELECT d.DISTRICT AS 'districtName',  h.HEALTH_FACILITY AS 'healthFacility',  DATE_FORMAT(s.PERFORMED_DATE, '%d-%m-%Y') AS 'performedDate',  CONCAT(t.NAME, ' ', t.SURNAME) AS 'tutorName',  f.NAME AS 'formName',  elaborado.BOOLEAN_VALUE AS 'elaborado',  aprovado.BOOLEAN_VALUE AS 'aprovado', 	revisado.BOOLEAN_VALUE AS 'revisado',  DATE_FORMAT(s.CREATED_AT, '%d-%m-%Y %H:%i:%s') AS 'createdAt' FROM INDICATORS s  INNER JOIN HEALTH_FACILITIES h  ON s.HEALTH_FACILITY_ID = h.ID  INNER JOIN TUTORS t  ON s.TUTOR_ID = t.ID  INNER JOIN FORMS f  ON s.FORM_ID = f.ID  INNER JOIN DISTRICTS d  ON h.DISTRICT_ID = d.ID  LEFT JOIN (SELECT INDICATOR_ID,  BOOLEAN_VALUE FROM ANSWERS a,  QUESTIONS q WHERE a.QUESTION_ID = q.ID  AND q.CODE = 'MTQ00000274') elaborado ON elaborado.INDICATOR_ID = s.ID  LEFT JOIN (SELECT INDICATOR_ID,  BOOLEAN_VALUE FROM ANSWERS a,  QUESTIONS q WHERE a.QUESTION_ID = q.ID  AND q.CODE = 'MTQ00000275') aprovado ON aprovado.INDICATOR_ID = s.ID 		LEFT JOIN (SELECT INDICATOR_ID,  BOOLEAN_VALUE FROM ANSWERS a,  QUESTIONS q WHERE a.QUESTION_ID = q.ID  AND q.CODE = 'MTQ00000276') revisado ON revisado.INDICATOR_ID = s.ID WHERE s.LIFE_CYCLE_STATUS = 'ACTIVE' AND s.PERFORMED_DATE >= '"+startDate.format(DateTimeFormatter.ofPattern(DATE_PATTERN))+"' AND s.PERFORMED_DATE <= '"+endDate.format(DateTimeFormatter.ofPattern(DATE_PATTERN))+"' AND f.NAME LIKE '%PROCEDIMENTO OPERACIONAL%' ORDER BY 1, 2, 3 ASC ");

		final Query query = this.getEntityManager().createNativeQuery(nativeQuery.toString());
		
		List<Object[]> performedSessionsHTS = query.getResultList();
		 
		List<PerformedSession> performedSessions= new ArrayList<PerformedSession>(0);
		
		for (Object[] ps : performedSessionsHTS) {
			PerformedSession p= new PerformedSession(
					ps[0].toString(), 
					ps[1].toString(), 
					ps[2].toString(), 
					ps[3].toString(), 
					ps[4].toString(), 
					ps[5].toString(), 
					ps[6].toString(), 
					ps[7].toString(), 
					ps[8].toString());
			
			performedSessions.add(p);
			
		}

		return performedSessions;
		
	}

	@Override
	public List<PerformedSession> findBySelectedFilterHTS(LocalDate startDate, LocalDate endDate, String tutoredUuid) {
		final StringBuilder nativeQuery = new StringBuilder();
		nativeQuery.append("SELECT d.DISTRICT AS 'districtName', h.HEALTH_FACILITY AS 'healthFacility', DATE_FORMAT(s.PERFORMED_DATE,'%d-%m-%Y') AS 'performedDate', CONCAT(t.NAME,' ',t.SURNAME) AS 'tutorName', CONCAT(tr.NAME,' ',tr.SURNAME) AS 'tutoredName', c.NAME AS 'cabinet', CASE m.DOOR WHEN 'P1' THEN '1' WHEN 'P2' THEN '2' WHEN 'P3' THEN '3' WHEN 'P4' THEN '4' END AS 'door', CASE m.TIME_OF_DAY WHEN 'DAY' THEN 'Dia' WHEN 'LATE_NIGHT' THEN 'Tarde/Noite' ELSE '' END AS 'timeOfDay', atendidos.NUMERIC_VALUE AS 'atendidos', previos.NUMERIC_VALUE AS 'previos', testados.NUMERIC_VALUE AS  'testados', positivos.NUMERIC_VALUE AS 'positivos', inscritos.NUMERIC_VALUE AS 'inscritos',DATE_FORMAT(s.CREATED_AT,'%d-%m-%Y %H:%i:%s') AS 'createdAt', c.ID AS 'cabinetId' FROM SESSIONS s INNER JOIN MENTORSHIPS m ON s.ID=m.SESSION_ID INNER JOIN HEALTH_FACILITIES h ON m.HEALTH_FACILITY_ID=h.ID INNER JOIN TUTORS t ON m.TUTOR_ID=t.ID INNER JOIN TUTOREDS tr ON m.TUTORED_ID=tr.ID INNER JOIN CABINETS c ON m.CABINET_ID=c.ID INNER JOIN FORMS f ON m.FORM_ID=f.ID INNER JOIN DISTRICTS d ON h.DISTRICT_ID = d.ID LEFT JOIN (SELECT MENTORSHIP_ID, NUMERIC_VALUE FROM ANSWERS a,QUESTIONS q WHERE a.QUESTION_ID=q.ID AND q.CODE='MTQ00000588') atendidos ON atendidos.MENTORSHIP_ID=m.ID LEFT JOIN (SELECT MENTORSHIP_ID, NUMERIC_VALUE FROM ANSWERS a,QUESTIONS q WHERE a.QUESTION_ID=q.ID AND q.CODE='MTQ00000589') previos ON previos.MENTORSHIP_ID=m.ID LEFT JOIN (SELECT MENTORSHIP_ID, NUMERIC_VALUE FROM ANSWERS a,QUESTIONS q WHERE a.QUESTION_ID=q.ID AND q.CODE='MTQ00000590') testados ON testados.MENTORSHIP_ID=m.ID LEFT JOIN (SELECT MENTORSHIP_ID, NUMERIC_VALUE FROM ANSWERS a,QUESTIONS q WHERE a.QUESTION_ID=q.ID AND q.CODE='MTQ00000591') positivos ON positivos.MENTORSHIP_ID=m.ID LEFT JOIN (SELECT MENTORSHIP_ID, NUMERIC_VALUE FROM ANSWERS a,QUESTIONS q WHERE a.QUESTION_ID=q.ID AND q.CODE='MTQ00000592') inscritos ON inscritos.MENTORSHIP_ID=m.ID  WHERE s.LIFE_CYCLE_STATUS='ACTIVE' AND m.LIFE_CYCLE_STATUS='ACTIVE' AND s.PERFORMED_DATE>='"+startDate.format(DateTimeFormatter.ofPattern(DATE_PATTERN))+"' AND s.PERFORMED_DATE<='"+endDate.format(DateTimeFormatter.ofPattern(DATE_PATTERN))+"' AND f.CODE='MT00000045' AND tr.UUID='"+tutoredUuid+"' ORDER BY 3 ASC");

		final Query query = this.getEntityManager().createNativeQuery(nativeQuery.toString());
		
		@SuppressWarnings("unchecked")
		List<Object[]> performedSessionsHTS = query.getResultList();
		 
		List<PerformedSession> performedSessions= new ArrayList<PerformedSession>(0);
		
		for (Object[] ps : performedSessionsHTS) {
			PerformedSession p= new PerformedSession(
					ps[0].toString(), 
					ps[1].toString(), 
					ps[2].toString(), 
					ps[3].toString(), 
					ps[4].toString(), 
					ps[5].toString(), 
					ps[6].toString(), 
					ps[7].toString(), 
					Long.valueOf(ps[8].toString()),
					Long.valueOf(ps[9].toString()), 
					Long.valueOf(ps[10].toString()), 
					Long.valueOf(ps[11].toString()), 
					Long.valueOf(ps[12].toString()), 
					ps[13].toString(),
					Long.valueOf(ps[14].toString()));
			
			performedSessions.add(p);
			
		}

		return performedSessions;
	}

	@Override
	public List<PerformedSession> findBySelectedFilterPMQTR(LocalDate startDate, LocalDate endDate) {
		final StringBuilder nativeQuery = new StringBuilder();
		nativeQuery.append("SELECT d.DISTRICT AS 'districtName', h.HEALTH_FACILITY AS 'healthFacility', DATE_FORMAT(s.PERFORMED_DATE, '%d-%m-%Y') AS 'performedDate', CONCAT(t.NAME, ' ', t.SURNAME) AS 'tutorName', CONCAT(tr.NAME, ' ', tr.SURNAME) AS 'tutoredName', c.NAME AS 'cabinet', ROUND((formacao.NUMERIC_VALUE/10)*100, 0) AS 'formacao', ROUND((instalacoes.NUMERIC_VALUE/5)*100, 0) AS 'instalacoes', ROUND((seguranca.NUMERIC_VALUE/11)*100, 0) AS 'seguranca',ROUND((pretestagem.NUMERIC_VALUE/12)*100, 0) AS 'pretestagem',ROUND((testagem.NUMERIC_VALUE/9)*100, 0) AS 'testagem',ROUND((postestagem.NUMERIC_VALUE/9)*100, 0) AS 'postestagem',ROUND((avaliacao.NUMERIC_VALUE/8)*100, 0) AS 'avaliacao',ROUND(((formacao.NUMERIC_VALUE+instalacoes.NUMERIC_VALUE+seguranca.NUMERIC_VALUE+pretestagem.NUMERIC_VALUE+testagem.NUMERIC_VALUE+postestagem.NUMERIC_VALUE+avaliacao.NUMERIC_VALUE)/64)*100, 0) AS 'total',s.CREATED_AT AS 'createdAt' , m.ID AS 'mentorship_id'FROM SESSIONS s INNER JOIN MENTORSHIPS m ON s.ID = m.SESSION_ID INNER JOIN HEALTH_FACILITIES h ON m.HEALTH_FACILITY_ID = h.ID INNER JOIN TUTORS t ON m.TUTOR_ID = t.ID INNER JOIN TUTOREDS tr ON m.TUTORED_ID = tr.ID INNER JOIN CABINETS c ON m.CABINET_ID = c.ID INNER JOIN FORMS f ON m.FORM_ID = f.ID INNER JOIN DISTRICTS d ON h.DISTRICT_ID = d.ID LEFT JOIN (SELECT MENTORSHIP_ID, SUM(CASE WHEN a.TEXT_VALUE = 'COMPETENTE' THEN 1 ELSE 0 end) AS numeric_value FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID = f.ID AND a.QUESTION_ID = q.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE' AND m.ID = a.MENTORSHIP_ID AND q.CODE IN ( 'MTQ00000751', 'MTQ00000752', 'MTQ00000753', 'MTQ00000754', 'MTQ00000755', 'MTQ00000756', 'MTQ00000757', 'MTQ00000758', 'MTQ00000759', 'MTQ00000760' ) AND f.CODE IN ( 'MT00000048' ) AND m.PERFORMED_DATE >= '"+startDate.format(DateTimeFormatter.ofPattern(DATE_PATTERN))+"' AND m.PERFORMED_DATE <= '"+endDate.format(DateTimeFormatter.ofPattern(DATE_PATTERN))+"' GROUP BY MENTORSHIP_ID) formacao ON formacao.MENTORSHIP_ID = m.ID LEFT JOIN (SELECT MENTORSHIP_ID, SUM(CASE WHEN a.TEXT_VALUE = 'COMPETENTE' THEN 1 ELSE 0 end) AS numeric_value FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID = f.ID AND a.QUESTION_ID = q.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE' AND m.ID = a.MENTORSHIP_ID AND q.CODE IN ( 'MTQ00000761', 'MTQ00000762', 'MTQ00000763', 'MTQ00000764', 'MTQ00000765' ) AND f.CODE IN ( 'MT00000048' ) AND m.PERFORMED_DATE >= '"+startDate.format(DateTimeFormatter.ofPattern(DATE_PATTERN))+"' AND m.PERFORMED_DATE <= '"+endDate.format(DateTimeFormatter.ofPattern(DATE_PATTERN))+"' GROUP BY MENTORSHIP_ID) instalacoes ON instalacoes.MENTORSHIP_ID = m.ID LEFT JOIN (SELECT MENTORSHIP_ID, SUM(CASE WHEN a.TEXT_VALUE = 'COMPETENTE' THEN 1 ELSE 0 end) AS numeric_value FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID = f.ID AND a.QUESTION_ID = q.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE' AND m.ID = a.MENTORSHIP_ID AND q.CODE IN ( 'MTQ00000766', 'MTQ00000767', 'MTQ00000768', 'MTQ00000769', 'MTQ00000770', 'MTQ00000771', 'MTQ00000772', 'MTQ00000773', 'MTQ00000774', 'MTQ00000775', 'MTQ00000776' ) AND f.CODE IN ( 'MT00000048' ) AND m.PERFORMED_DATE >= '"+startDate.format(DateTimeFormatter.ofPattern(DATE_PATTERN))+"' AND m.PERFORMED_DATE <= '"+endDate.format(DateTimeFormatter.ofPattern(DATE_PATTERN))+"' GROUP BY MENTORSHIP_ID) seguranca ON seguranca.MENTORSHIP_ID = m.ID LEFT JOIN (SELECT MENTORSHIP_ID, SUM(CASE WHEN a.TEXT_VALUE = 'COMPETENTE' THEN 1 ELSE 0 end) AS numeric_value FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID = f.ID AND a.QUESTION_ID = q.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE' AND m.ID = a.MENTORSHIP_ID AND q.CODE IN ( 'MTQ00000777', 'MTQ00000778', 'MTQ00000779', 'MTQ00000780', 'MTQ00000781', 'MTQ00000782', 'MTQ00000783', 'MTQ00000784', 'MTQ00000785', 'MTQ00000786', 'MTQ00000787', 'MTQ00000788' ) AND m.PERFORMED_DATE >= '"+startDate.format(DateTimeFormatter.ofPattern(DATE_PATTERN))+"' AND m.PERFORMED_DATE <= '"+endDate.format(DateTimeFormatter.ofPattern(DATE_PATTERN))+"' GROUP BY MENTORSHIP_ID) pretestagem ON pretestagem.MENTORSHIP_ID = m.ID LEFT JOIN (SELECT MENTORSHIP_ID, SUM(CASE WHEN a.TEXT_VALUE = 'COMPETENTE' THEN 1 ELSE 0 end) AS numeric_value FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID = f.ID AND a.QUESTION_ID = q.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE' AND m.ID = a.MENTORSHIP_ID AND q.CODE IN ( 'MTQ00000789', 'MTQ00000790', 'MTQ00000791', 'MTQ00000792', 'MTQ00000793', 'MTQ00000794', 'MTQ00000795', 'MTQ00000796', 'MTQ00000797' ) AND m.PERFORMED_DATE >= '"+startDate.format(DateTimeFormatter.ofPattern(DATE_PATTERN))+"' AND m.PERFORMED_DATE <= '"+endDate.format(DateTimeFormatter.ofPattern(DATE_PATTERN))+"' GROUP BY MENTORSHIP_ID) testagem ON testagem.MENTORSHIP_ID = m.ID LEFT JOIN (SELECT MENTORSHIP_ID, SUM(CASE WHEN a.TEXT_VALUE = 'COMPETENTE' THEN 1 ELSE 0 end) AS numeric_value FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID = f.ID AND a.QUESTION_ID = q.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE' AND m.ID = a.MENTORSHIP_ID AND q.CODE IN ( 'MTQ00000798', 'MTQ00000799', 'MTQ00000800', 'MTQ00000801', 'MTQ00000802', 'MTQ00000803', 'MTQ00000804', 'MTQ00000805', 'MTQ00000806' ) AND m.PERFORMED_DATE >= '"+startDate.format(DateTimeFormatter.ofPattern(DATE_PATTERN))+"' AND m.PERFORMED_DATE <= '"+endDate.format(DateTimeFormatter.ofPattern(DATE_PATTERN))+"' GROUP BY MENTORSHIP_ID) postestagem ON postestagem.MENTORSHIP_ID = m.ID LEFT JOIN (SELECT MENTORSHIP_ID, SUM(CASE WHEN a.TEXT_VALUE = 'COMPETENTE' THEN 1 ELSE 0 end) AS numeric_value FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID = f.ID AND a.QUESTION_ID = q.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE' AND m.ID = a.MENTORSHIP_ID AND q.CODE IN ( 'MTQ00000807', 'MTQ00000808', 'MTQ00000809', 'MTQ00000810', 'MTQ00000811', 'MTQ00000812', 'MTQ00000813', 'MTQ00000814' ) AND m.PERFORMED_DATE >= '"+startDate.format(DateTimeFormatter.ofPattern(DATE_PATTERN))+"' AND m.PERFORMED_DATE <= '"+endDate.format(DateTimeFormatter.ofPattern(DATE_PATTERN))+"' GROUP BY MENTORSHIP_ID) avaliacao ON avaliacao.MENTORSHIP_ID = m.ID WHERE s.LIFE_CYCLE_STATUS = 'ACTIVE' AND m.LIFE_CYCLE_STATUS = 'ACTIVE' AND s.PERFORMED_DATE >= '"+startDate.format(DateTimeFormatter.ofPattern(DATE_PATTERN))+"' AND s.PERFORMED_DATE <= '"+endDate.format(DateTimeFormatter.ofPattern(DATE_PATTERN))+"' AND f.CODE = 'MT00000048' ORDER BY 1, 2, 3 ASC ");

		final Query query = this.getEntityManager().createNativeQuery(nativeQuery.toString());
		
		@SuppressWarnings("unchecked")
		List<Object[]> performedSessionsHTS = query.getResultList();
		 
		List<PerformedSession> performedSessions= new ArrayList<PerformedSession>(0);
		
		for (Object[] ps : performedSessionsHTS) {
			PerformedSession p= new PerformedSession(
					ps[0].toString(), 
					ps[1].toString(), 
					ps[2].toString(), 
					ps[3].toString(), 
					ps[4].toString(), 
					ps[5].toString(), 
					Integer.valueOf(ps[6].toString()), 
					Integer.valueOf(ps[7].toString()),
					Integer.valueOf(ps[8].toString()), 
					Integer.valueOf(ps[9].toString()), 
					Integer.valueOf(ps[10].toString()), 
					Integer.valueOf(ps[11].toString()), 
					Integer.valueOf(ps[12].toString()), 
					Integer.valueOf(ps[13].toString()), 
					ps[14].toString(),
					Long.valueOf(ps[15].toString()));
			
			performedSessions.add(p);
			
		}

		return performedSessions;
	}

	@Override
	public List<PerformedSession> findBySelectedFilterNarrativeCOP20(LocalDate startDate, LocalDate endDate) {
		
	final StringBuilder nativeQuery = new StringBuilder();
		
		String start=startDate.format(DateTimeFormatter.ofPattern(DATE_PATTERN));
		String end=endDate.format(DateTimeFormatter.ofPattern(DATE_PATTERN));
		
		nativeQuery.append("SELECT d.PROVINCE, (SELECT COUNT(DISTINCT( m.SESSION_ID )) FROM MENTORSHIPS m, FORMS f, CABINETS c WHERE m.FORM_ID = f.ID AND m.CABINET_ID = c.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE' AND c.ID=14 AND f.CODE IN('MT00000034','MT00000049') AND m.PERFORMED_DATE >= '"+start+"' AND m.PERFORMED_DATE <= '"+end+"') AS 'ind_11061',  (SELECT COUNT(DISTINCT( m.SESSION_ID )) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID = f.ID AND a.QUESTION_ID = q.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE' AND m.ID = a.MENTORSHIP_ID AND q.CODE IN ('MTQ00000837' ,'MTQ00000838') AND f.CODE = 'MT00000049' AND m.PERFORMED_DATE >= '"+start+"' AND m.PERFORMED_DATE <= '"+end+"' AND EXISTS (SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID = m.ID AND a2.TEXT_VALUE IN ( 'COMPETENTE', 'NAO SATISFATORIO' ))) AS 'ind_11011',   (SELECT COUNT(DISTINCT( m.SESSION_ID )) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID = f.ID AND a.QUESTION_ID = q.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE' AND m.ID = a.MENTORSHIP_ID AND q.CODE IN ('MTQ00000839','MTQ00000898','MTQ00000899','MTQ00000900') AND f.CODE = 'MT00000049' AND m.PERFORMED_DATE >= '"+start+"' AND m.PERFORMED_DATE <= '"+end+"' AND EXISTS (SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID = m.ID AND a2.TEXT_VALUE IN ( 'COMPETENTE', 'NAO SATISFATORIO' ))) AS 'ind_11031',   (SELECT COUNT(DISTINCT( m.SESSION_ID )) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a , CABINETS c WHERE m.FORM_ID = f.ID  AND m.CABINET_ID = c.ID AND c.ID IN(6,10) AND a.QUESTION_ID = q.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE' AND m.ID = a.MENTORSHIP_ID AND q.CODE IN ('MTQ00000837' ,'MTQ00000838') AND f.CODE = 'MT00000049' AND m.PERFORMED_DATE >= '"+start+"' AND m.PERFORMED_DATE <= '"+end+"' AND EXISTS (SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID = m.ID AND a2.TEXT_VALUE IN ( 'COMPETENTE', 'NAO SATISFATORIO' ))) AS 'ind_11041',   (SELECT COUNT(DISTINCT( m.SESSION_ID )) FROM MENTORSHIPS m, FORMS f, CABINETS c WHERE m.FORM_ID = f.ID AND m.CABINET_ID = c.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE' AND c.ID IN (6,10, 12) AND f.CODE IN('MT00000034','MT00000049') AND m.PERFORMED_DATE >= '"+start+"' AND m.PERFORMED_DATE <= '"+end+"') AS 'ind_11043',   (SELECT COUNT(DISTINCT( m.SESSION_ID )) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID = f.ID AND a.QUESTION_ID = q.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE' AND m.ID = a.MENTORSHIP_ID AND q.CODE IN ('MTQ00000895' ,'MTQ00000904') AND f.CODE = 'MT00000049' AND m.PERFORMED_DATE >= '"+start+"' AND m.PERFORMED_DATE <= '"+end+"' AND EXISTS (SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID = m.ID AND a2.TEXT_VALUE IN ( 'COMPETENTE', 'NAO SATISFATORIO' ))) AS 'ind_11073',   (SELECT COUNT(DISTINCT( m.SESSION_ID )) FROM MENTORSHIPS m, FORMS f, CABINETS c WHERE m.FORM_ID = f.ID AND m.CABINET_ID = c.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE' AND c.ID=12 AND f.CODE IN('MT00000034','MT00000049') AND m.PERFORMED_DATE >= '"+start+"' AND m.PERFORMED_DATE <= '"+end+"') AS 'ind_42',   (SELECT COUNT(DISTINCT( m.SESSION_ID )) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID = f.ID AND a.QUESTION_ID = q.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE' AND m.ID = a.MENTORSHIP_ID AND q.CODE IN ('MTQ00000827','MTQ00000833','MTQ00000905','MTQ00000909','MTQ00000910') AND f.CODE = 'MT00000049' AND m.PERFORMED_DATE >= '"+start+"' AND m.PERFORMED_DATE <= '"+end+"' AND EXISTS (SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID = m.ID AND a2.TEXT_VALUE IN ( 'COMPETENTE', 'NAO SATISFATORIO' ))) AS 'ind_10043',   (SELECT COUNT(DISTINCT( m.SESSION_ID )) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a , CABINETS c WHERE m.FORM_ID = f.ID  AND m.CABINET_ID = c.ID AND c.ID =12 AND a.QUESTION_ID = q.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE' AND m.ID = a.MENTORSHIP_ID AND q.CODE IN ('MTQ00000832' ,'MTQ00000312','MTQ00000313','MTQ00000314','MTQ00000344','MTQ00000353') AND f.CODE IN ('MT00000049' ,'MT00000035','MT00000036','MT00000038') AND m.PERFORMED_DATE >= '"+start+"' AND m.PERFORMED_DATE <= '"+end+"' AND EXISTS (SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID = m.ID AND a2.TEXT_VALUE IN ( 'COMPETENTE', 'NAO SATISFATORIO' ))) AS 'ind_10045',   (SELECT COUNT(DISTINCT( m.SESSION_ID )) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a , CABINETS c WHERE m.FORM_ID = f.ID  AND m.CABINET_ID = c.ID AND c.ID =4 AND a.QUESTION_ID = q.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE' AND m.ID = a.MENTORSHIP_ID AND q.CODE IN ('MTQ00000826' ,'MTQ00000834') AND f.CODE='MT00000049' AND m.PERFORMED_DATE >= '"+start+"' AND m.PERFORMED_DATE <= '"+end+"' AND EXISTS (SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID = m.ID AND a2.TEXT_VALUE IN ( 'COMPETENTE', 'NAO SATISFATORIO' ))) AS 'ind_04071',   (SELECT COUNT(DISTINCT( m.SESSION_ID )) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a , CABINETS c WHERE m.FORM_ID = f.ID  AND m.CABINET_ID = c.ID AND c.ID IN (4, 5, 22) AND a.QUESTION_ID = q.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE' AND m.ID = a.MENTORSHIP_ID AND q.CODE IN ('MTQ00000839','MTQ00000898','MTQ00000899','MTQ00000900') AND f.CODE='MT00000049' AND m.PERFORMED_DATE >= '"+start+"' AND m.PERFORMED_DATE <= '"+end+"' AND EXISTS (SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID = m.ID AND a2.TEXT_VALUE IN ( 'COMPETENTE', 'NAO SATISFATORIO' ))) AS 'ind_04073',   (SELECT COUNT(DISTINCT( m.SESSION_ID )) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a , CABINETS c WHERE m.FORM_ID = f.ID  AND m.CABINET_ID = c.ID AND c.ID IN (5, 22) AND a.QUESTION_ID = q.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE' AND m.ID = a.MENTORSHIP_ID AND q.CODE IN ('MTQ00000837','MTQ00000906') AND f.CODE='MT00000049' AND m.PERFORMED_DATE >= '"+start+"' AND m.PERFORMED_DATE <= '"+end+"' AND EXISTS (SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID = m.ID AND a2.TEXT_VALUE IN ( 'COMPETENTE', 'NAO SATISFATORIO' ))) AS 'ind_04041',   (SELECT COUNT(DISTINCT( m.SESSION_ID )) FROM MENTORSHIPS m, FORMS f, CABINETS c WHERE m.FORM_ID = f.ID AND m.CABINET_ID = c.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE' AND c.ID IN (4,5,22) AND f.CODE IN('MT00000047') AND m.PERFORMED_DATE >= '"+start+"' AND m.PERFORMED_DATE <= '"+end+"') AS 'ind_04061',   (SELECT COUNT(DISTINCT( m.SESSION_ID )) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID = f.ID AND a.QUESTION_ID = q.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE' AND m.ID = a.MENTORSHIP_ID AND q.CODE='MTQ00000838' AND f.CODE='MT00000049' AND m.PERFORMED_DATE >= '"+start+"' AND m.PERFORMED_DATE <= '"+end+"' AND EXISTS (SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID = m.ID AND a2.TEXT_VALUE IN ( 'COMPETENTE', 'NAO SATISFATORIO' ))) AS 'ind_15051',   (SELECT COUNT(DISTINCT( m.SESSION_ID )) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID = f.ID AND a.QUESTION_ID = q.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE' AND m.ID = a.MENTORSHIP_ID AND q.CODE IN ('MTQ00000832' ,'MTQ00000345') AND f.CODE IN ('MT00000049' ,'MT00000035','MT00000036','MT00000038') AND m.PERFORMED_DATE >= '"+start+"' AND m.PERFORMED_DATE <= '"+end+"' AND EXISTS (SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID = m.ID AND a2.TEXT_VALUE IN ( 'COMPETENTE', 'NAO SATISFATORIO' ))) AS 'ind_06044',   (SELECT COUNT(DISTINCT( m.SESSION_ID )) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID = f.ID AND a.QUESTION_ID = q.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE' AND m.ID = a.MENTORSHIP_ID AND q.CODE IN ('MTQ00000329','MTQ00000311','MTQ00000512','MTQ00000331') AND f.CODE IN ('MT00000035','MT00000036','MT00000038','MT00000043','MT00000037') AND m.PERFORMED_DATE >= '"+start+"' AND m.PERFORMED_DATE <= '"+end+"' AND EXISTS (SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID = m.ID AND a2.TEXT_VALUE IN ( 'COMPETENTE', 'NAO SATISFATORIO' ))) AS 'ind_02041',   (SELECT COUNT(DISTINCT( m.SESSION_ID )) FROM MENTORSHIPS m, FORMS f WHERE m.FORM_ID = f.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE' AND f.CODE IN('MT00000046','MT00000047') AND m.PERFORMED_DATE >= '"+start+"' AND m.PERFORMED_DATE <= '"+end+"') AS 'ind_01102',   (SELECT COUNT(DISTINCT( m.SESSION_ID )) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID = f.ID AND a.QUESTION_ID = q.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE' AND m.ID = a.MENTORSHIP_ID AND q.CODE IN ('MTQ00000332','MTQ00000736','MTQ00000527') AND f.CODE IN ('MT00000035','MT00000046','MT00000043','MT00000037') AND m.PERFORMED_DATE >= '"+start+"' AND m.PERFORMED_DATE <= '"+end+"' AND EXISTS (SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID = m.ID AND a2.TEXT_VALUE IN ( 'COMPETENTE', 'NAO SATISFATORIO' ))) AS 'ind_01031',   (SELECT COUNT(DISTINCT( m.SESSION_ID )) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID = f.ID AND a.QUESTION_ID = q.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE' AND m.ID = a.MENTORSHIP_ID AND q.CODE IN ('MTQ00000317','MTQ00000318','MTQ00000319','MTQ00000680','MTQ00000710','MTQ00000711','MTQ00000740','MTQ00000521') AND f.CODE IN ('MT00000035','MT00000036','MT00000038','MT00000043','MT00000037','MT00000047') AND m.PERFORMED_DATE >= '"+start+"' AND m.PERFORMED_DATE <= '"+end+"' AND EXISTS (SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID = m.ID AND a2.TEXT_VALUE IN ( 'COMPETENTE', 'NAO SATISFATORIO' ))) AS 'ind_01142',   (SELECT COUNT(DISTINCT( m.SESSION_ID )) FROM MENTORSHIPS m, FORMS f, CABINETS c WHERE m.FORM_ID = f.ID AND m.CABINET_ID = c.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE' AND c.ID=12 AND f.CODE IN('MT00000035','MT00000036','MT00000038','MT00000047') AND m.PERFORMED_DATE >= '"+start+"' AND m.PERFORMED_DATE <= '"+end+"') AS 'ind_02063',   (SELECT COUNT(DISTINCT( m.SESSION_ID )) FROM MENTORSHIPS m, FORMS f WHERE m.FORM_ID = f.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE'  AND m.HEALTH_FACILITY_ID = 128 AND f.CODE IN('MT00000035','MT00000036','MT00000038','MT00000047') AND m.PERFORMED_DATE >= '"+start+"' AND m.PERFORMED_DATE <= '"+end+"') AS 'ind_01116',   (SELECT COUNT(DISTINCT( m.SESSION_ID )) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID = f.ID AND a.QUESTION_ID = q.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE' AND m.ID = a.MENTORSHIP_ID AND q.CODE IN ('MTQ00000341','MTQ00000722','MTQ00000723','MTQ00000724','MTQ00000725','MTQ00000726') AND f.CODE IN ('MT00000035','MT00000047') AND m.PERFORMED_DATE >= '"+start+"' AND m.PERFORMED_DATE <= '"+end+"' AND EXISTS (SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID = m.ID AND a2.TEXT_VALUE IN ( 'COMPETENTE', 'NAO SATISFATORIO' ))) AS 'ind_02071',   (SELECT COUNT(DISTINCT( m.SESSION_ID )) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a , CABINETS c WHERE m.FORM_ID = f.ID AND a.QUESTION_ID = q.ID  AND m.CABINET_ID = c.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE'  AND c.ID IN (4, 5, 22) AND m.ID = a.MENTORSHIP_ID AND q.CODE IN ('MTQ00000382','MTQ00000341','MTQ00000722','MTQ00000723','MTQ00000724','MTQ00000725','MTQ00000726') AND f.CODE IN ('MT00000036','MT00000038','MT00000047') AND m.PERFORMED_DATE >= '"+start+"' AND m.PERFORMED_DATE <= '"+end+"' AND EXISTS (SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID = m.ID AND a2.TEXT_VALUE IN ( 'COMPETENTE', 'NAO SATISFATORIO' ))) AS 'ind_02021',   (SELECT COUNT(DISTINCT( m.SESSION_ID )) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a , CABINETS c WHERE m.FORM_ID = f.ID AND a.QUESTION_ID = q.ID  AND m.CABINET_ID = c.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE'  AND c.ID IN (4, 5, 22) AND m.ID = a.MENTORSHIP_ID AND q.CODE IN ('MTQ00000382','MTQ00000341','MTQ00000722','MTQ00000723','MTQ00000724','MTQ00000725','MTQ00000726') AND f.CODE IN ('MT00000036','MT00000038','MT00000047') AND m.PERFORMED_DATE >= '"+start+"' AND m.PERFORMED_DATE <= '"+end+"' AND EXISTS (SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID = m.ID AND a2.TEXT_VALUE IN ( 'COMPETENTE', 'NAO SATISFATORIO' ))) AS 'ind_02023',   (SELECT COUNT(DISTINCT( m.SESSION_ID )) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID = f.ID AND a.QUESTION_ID = q.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE' AND m.ID = a.MENTORSHIP_ID AND q.CODE IN ('MTQ00000380','MTQ00000509','MTQ00000328') AND f.CODE IN ('MT00000035','MT00000036','MT00000038','MT00000037','MT00000039') AND m.PERFORMED_DATE >= '"+start+"' AND m.PERFORMED_DATE <= '"+end+"' AND EXISTS (SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID = m.ID AND a2.TEXT_VALUE IN ( 'COMPETENTE', 'NAO SATISFATORIO' ))) AS 'ind_03029',   (SELECT COUNT(DISTINCT( m.SESSION_ID )) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID = f.ID AND a.QUESTION_ID = q.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE' AND m.ID = a.MENTORSHIP_ID AND q.CODE IN ('MTQ00000380','MTQ00000509','MTQ00000328') AND f.CODE IN ('MT00000035','MT00000036','MT00000038','MT00000037','MT00000039') AND m.PERFORMED_DATE >= '"+start+"' AND m.PERFORMED_DATE <= '"+end+"' AND EXISTS (SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID = m.ID AND a2.TEXT_VALUE IN ( 'COMPETENTE', 'NAO SATISFATORIO' ))) AS 'ind_03011',   (SELECT COUNT(DISTINCT( m.SESSION_ID )) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID = f.ID AND a.QUESTION_ID = q.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE' AND m.ID = a.MENTORSHIP_ID AND q.CODE IN ('MTQ00000385','MTQ00000335') AND f.CODE IN ('MT00000035','MT00000036','MT00000038','MT00000037') AND m.PERFORMED_DATE >= '"+start+"' AND m.PERFORMED_DATE <= '"+end+"' AND EXISTS (SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID = m.ID AND a2.TEXT_VALUE IN ( 'COMPETENTE', 'NAO SATISFATORIO' ))) AS 'ind_03013',   (SELECT COUNT(DISTINCT( m.SESSION_ID )) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID = f.ID AND a.QUESTION_ID = q.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE' AND m.ID = a.MENTORSHIP_ID AND q.CODE IN ('MTQ00000607','MTQ00000639','MTQ00000640','MTQ00000641') AND f.CODE ='MT00000046' AND m.PERFORMED_DATE >= '"+start+"' AND m.PERFORMED_DATE <= '"+end+"' AND EXISTS (SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID = m.ID AND a2.TEXT_VALUE IN ( 'COMPETENTE', 'NAO SATISFATORIO' ))) AS 'ind_05012',   (SELECT COUNT(DISTINCT( m.SESSION_ID )) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID = f.ID AND a.QUESTION_ID = q.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE' AND m.ID = a.MENTORSHIP_ID AND q.CODE IN ('MTQ00000311','MTQ00000329') AND f.CODE ='MT00000037' AND m.PERFORMED_DATE >= '"+start+"' AND m.PERFORMED_DATE <= '"+end+"' AND EXISTS (SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID = m.ID AND a2.TEXT_VALUE IN ( 'COMPETENTE', 'NAO SATISFATORIO' ))) AS 'ind_05031',   (SELECT COUNT(DISTINCT( m.SESSION_ID )) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID = f.ID AND a.QUESTION_ID = q.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE' AND m.ID = a.MENTORSHIP_ID AND q.CODE IN ('MTQ00000332','MTQ00000333') AND f.CODE ='MT00000037' AND m.PERFORMED_DATE >= '"+start+"' AND m.PERFORMED_DATE <= '"+end+"' AND EXISTS (SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID = m.ID AND a2.TEXT_VALUE IN ( 'COMPETENTE', 'NAO SATISFATORIO' ))) AS 'ind_05061',   (SELECT COUNT(DISTINCT( m.SESSION_ID )) FROM MENTORSHIPS m, FORMS f WHERE m.FORM_ID = f.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE' AND f.CODE='MT00000046' AND m.PERFORMED_DATE >= '"+start+"' AND m.PERFORMED_DATE <= '"+end+"') AS 'ind_05052',   (SELECT COUNT(DISTINCT( m.SESSION_ID )) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID = f.ID AND a.QUESTION_ID = q.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE' AND m.ID = a.MENTORSHIP_ID AND q.CODE='MTQ00000616' AND f.CODE ='MT00000046' AND m.PERFORMED_DATE >= '"+start+"' AND m.PERFORMED_DATE <= '"+end+"' AND EXISTS (SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID = m.ID AND a2.TEXT_VALUE IN ( 'COMPETENTE', 'NAO SATISFATORIO' ))) AS 'ind_05054',   (SELECT COUNT(DISTINCT( m.SESSION_ID )) FROM MENTORSHIPS m, FORMS f, QUESTIONS q, ANSWERS a WHERE m.FORM_ID = f.ID AND a.QUESTION_ID = q.ID AND m.LIFE_CYCLE_STATUS = 'ACTIVE' AND m.ID = a.MENTORSHIP_ID AND q.CODE IN ('MTQ00000607','MTQ00000639','MTQ00000640','MTQ00000641') AND f.CODE ='MT00000046' AND m.PERFORMED_DATE >= '"+start+"' AND m.PERFORMED_DATE <= '"+end+"' AND EXISTS (SELECT a2.ID FROM ANSWERS a2 WHERE a2.MENTORSHIP_ID = m.ID AND a2.TEXT_VALUE IN ( 'COMPETENTE', 'NAO SATISFATORIO' ))) AS 'ind_05057' FROM DISTRICTS d GROUP BY PROVINCE");

		final Query query = this.getEntityManager().createNativeQuery(nativeQuery.toString());
		
		List<Object[]> performedSessionsHTS = query.getResultList();
		 
		List<PerformedSession> performedSessions= new ArrayList<PerformedSession>(0);
		
		for (Object[] ps : performedSessionsHTS) {
			PerformedSession p= new PerformedSession(
					ps[0].toString(),
					Long.valueOf(ps[1].toString()), 
					Long.valueOf(ps[2].toString()),
					Long.valueOf(ps[3].toString()),
					Long.valueOf(ps[4].toString()),
					Long.valueOf(ps[5].toString()),
					Long.valueOf(ps[6].toString()),
					Long.valueOf(ps[7].toString()),
					Long.valueOf(ps[8].toString()),
					Long.valueOf(ps[9].toString()),
					Long.valueOf(ps[10].toString()),
					Long.valueOf(ps[11].toString()),
					Long.valueOf(ps[12].toString()),
					Long.valueOf(ps[13].toString()),
					Long.valueOf(ps[14].toString()),
					Long.valueOf(ps[15].toString()),
					Long.valueOf(ps[16].toString()),
					Long.valueOf(ps[17].toString()),
					Long.valueOf(ps[18].toString()),
					Long.valueOf(ps[19].toString()),
					Long.valueOf(ps[20].toString()),
					Long.valueOf(ps[21].toString()),
					Long.valueOf(ps[22].toString()),
					Long.valueOf(ps[23].toString()),
					Long.valueOf(ps[24].toString()),
					Long.valueOf(ps[25].toString()),
					Long.valueOf(ps[26].toString()),
					Long.valueOf(ps[27].toString()),
					Long.valueOf(ps[28].toString()),
					Long.valueOf(ps[29].toString()),
					Long.valueOf(ps[30].toString()),
					Long.valueOf(ps[31].toString()),
					Long.valueOf(ps[32].toString()),
					Long.valueOf(ps[33].toString())
);
			
			performedSessions.add(p);
			
		}

		return performedSessions;
	}
}
