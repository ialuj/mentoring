/*
 * Friends in Global Health - FGH © 2016
 */
package mz.org.fgh.mentoring.core.tutor.service;

import java.util.List;

import mz.co.mozview.frameworks.core.exception.BusinessException;
import mz.co.mozview.frameworks.core.webservices.model.UserContext;
import mz.org.fgh.mentoring.core.career.model.CareerType;
import mz.org.fgh.mentoring.core.tutor.model.Tutor;

/**
 * @author Eusebio Jose Maposse
 *
 */
public interface TutorQueryService {

	String NAME = "mz.org.fgh.mentoring.core.tutor.service.TutorQueryService";

	List<Tutor> findTutorsBySelectedFilter(final UserContext userContext, final String code, final String name,
	        final String surname, CareerType careerType, final String phoneNumber) throws BusinessException;
	
	List<Tutor> findTutorsBySelectedFilter(final UserContext userContext, final String code, final String name,
	        final String surname, CareerType careerType, final String phoneNumber, final String partnerUuid) throws BusinessException;

	Tutor fetchTutorByUuid(final UserContext userContext, final String uuid) throws BusinessException;

	Tutor fetchTutorByEmail(final UserContext userContext, final String email) throws BusinessException;
	
}
