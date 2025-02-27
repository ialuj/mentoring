/*
 * Friends in Global Health - FGH © 2016
 */
package mz.org.fgh.mentoring.core.tutor.dao;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.stereotype.Repository;

import mz.co.mozview.frameworks.core.dao.GenericDAOImpl;
import mz.co.mozview.frameworks.core.dao.ParamBuilder;
import mz.co.mozview.frameworks.core.util.LifeCycleStatus;
import mz.org.fgh.mentoring.core.career.model.CareerType;
import mz.org.fgh.mentoring.core.tutor.model.Tutor;

/**
 * @author Stélio Moiane
 *
 */
@Repository(TutorDAO.NAME)
public class TutorDAOImpl extends GenericDAOImpl<Tutor, Long> implements TutorDAO {

	@Override
	public List<Tutor> findBySelectedFilter(final String code, final String name, final String surname,
	        final String phoneNumber, final CareerType careerType, final LifeCycleStatus lifeCycleStatus) {

		final CriteriaBuilder criteriaBuilder = this.getEntityManager().getCriteriaBuilder();
		final CriteriaQuery<Tutor> createQuery = criteriaBuilder.createQuery(Tutor.class);
		final Root<Tutor> root = createQuery.from(Tutor.class);
		root.fetch("career");
		root.fetch("partner");

		createQuery.select(root);

		final List<Predicate> predicates = new ArrayList<>();

		if (code != null) {
			predicates.add(criteriaBuilder.like(root.get("code"), "%" + code + "%"));
		}

		if (name != null) {
			predicates.add(criteriaBuilder.like(root.get("name"), "%" + name + "%"));
		}

		if (surname != null) {
			predicates.add(criteriaBuilder.like(root.get("surname"), "%" + surname + "%"));
		}

		if (careerType != null) {
			predicates.add(criteriaBuilder.equal(root.get("career").get("careerType"), careerType));
		}
		if (phoneNumber != null) {
			predicates.add(criteriaBuilder.equal(root.get("phoneNumber"), phoneNumber));
		}

		predicates.add(criteriaBuilder.equal(root.get("lifeCycleStatus"), lifeCycleStatus));
		createQuery.where(predicates.toArray(new Predicate[predicates.size()]))
		        .orderBy(criteriaBuilder.asc(root.get("name")));

		final TypedQuery<Tutor> query = this.getEntityManager().createQuery(createQuery);

		return query.getResultList();
	}

	@Override
	public Tutor fetchByUuid(final String uuid) {
		return this.findSingleByNamedQuery(TutorDAO.QUERY_NAME.fetchByUuid,
		        new ParamBuilder().add("uuid", uuid).process());
	}

	@Override
	public Tutor fecthByEmail(final String email, final LifeCycleStatus lifeCycleStatus) {
		return this.findSingleByNamedQuery(TutorDAO.QUERY_NAME.fetchByEmail,
		        new ParamBuilder().add("email", email).add("lifeCycleStatus", lifeCycleStatus).process());
	}

	@Override
	public List<Tutor> findBySelectedFilter(String code, String name, String surname, String phoneNumber,
			String partnerUuid, CareerType careerType, LifeCycleStatus lifeCycleStatus) {
		
		final CriteriaBuilder criteriaBuilder = this.getEntityManager().getCriteriaBuilder();
		final CriteriaQuery<Tutor> createQuery = criteriaBuilder.createQuery(Tutor.class);
		final Root<Tutor> root = createQuery.from(Tutor.class);
		root.fetch("career");
		root.fetch("partner");

		createQuery.select(root);

		final List<Predicate> predicates = new ArrayList<>();

		if (code != null) {
			predicates.add(criteriaBuilder.like(root.get("code"), "%" + code + "%"));
		}

		if (name != null) {
			predicates.add(criteriaBuilder.like(root.get("name"), "%" + name + "%"));
		}

		if (surname != null) {
			predicates.add(criteriaBuilder.like(root.get("surname"), "%" + surname + "%"));
		}

		if (careerType != null) {
			predicates.add(criteriaBuilder.equal(root.get("career").get("careerType"), careerType));
		}
		if (phoneNumber != null) {
			predicates.add(criteriaBuilder.equal(root.get("phoneNumber"), phoneNumber));
		}
		
		if (partnerUuid != null) {
			predicates.add(criteriaBuilder.equal(root.get("partner").get("uuid"), partnerUuid));
		}

		predicates.add(criteriaBuilder.equal(root.get("lifeCycleStatus"), lifeCycleStatus));
		createQuery.where(predicates.toArray(new Predicate[predicates.size()]))
		        .orderBy(criteriaBuilder.asc(root.get("name")));

		final TypedQuery<Tutor> query = this.getEntityManager().createQuery(createQuery);

		return query.getResultList();
	}

}
