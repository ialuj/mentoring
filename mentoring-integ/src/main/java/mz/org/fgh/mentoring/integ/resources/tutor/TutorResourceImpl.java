/*
 * Friends in Global Health - FGH © 2016
 */
package mz.org.fgh.mentoring.integ.resources.tutor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.Path;

import mz.org.fgh.mentoring.core.location.model.HealthFacility;
import mz.org.fgh.mentoring.core.partner.model.Partner;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.sun.jersey.api.JResponse;

import mz.co.mozview.frameworks.core.exception.BusinessException;
import mz.co.mozview.frameworks.core.webservices.model.UserContext;
import mz.org.fgh.mentoring.core.career.model.CareerType;
import mz.org.fgh.mentoring.core.tutor.model.Tutor;
import mz.org.fgh.mentoring.core.tutor.model.TutorLocation;
import mz.org.fgh.mentoring.core.tutor.service.TutorLocationService;
import mz.org.fgh.mentoring.core.tutor.service.TutorQueryService;
import mz.org.fgh.mentoring.core.tutor.service.TutorService;
import mz.org.fgh.mentoring.integ.resources.AbstractResource;

/**
 * @author Stélio Moiane
 *
 */
@Service(TutorResource.NAME)
@Path("tutors")
public class TutorResourceImpl extends AbstractResource implements TutorResource {

	@Inject
	private TutorService tutorService;

	@Inject
	private TutorQueryService tutorQueryService;

	@Inject
	private TutorLocationService tutorLocationService;

	@Override
	public JResponse<Tutor> createTutor(final TutorBeanResource tutorBeanResource) throws BusinessException {

		final Tutor tutor = tutorBeanResource.getTutor();

		try {
			this.tutorQueryService.fetchTutorByUuid(tutorBeanResource.getUserContext(), tutor.getUuid());
		} catch (final BusinessException ex) {
			this.tutorService.createTutor(tutorBeanResource.getUserContext(), tutor);
		}

		return JResponse.ok(tutor).build();
	}

	@Override
	public JResponse<List<Tutor>> findTutors(final String code, final String name, final String surname,
			final CareerType careerType, final String phoneNumber) throws BusinessException {

		final List<Tutor> tutors = this.tutorQueryService.findTutorsBySelectedFilter(this.getUserContetx(), code, name,
				surname, careerType, phoneNumber);

		return JResponse.ok(tutors).build();
	}

	@Override
	public JResponse<Tutor> updateTutor(final TutorBeanResource tutorBeanResource) throws BusinessException {

		final Tutor tutor = this.tutorService.updateTutor(tutorBeanResource.getUserContext(),
				tutorBeanResource.getTutor());

		return JResponse.ok(tutor).build();
	}

	@Override
	public JResponse<Tutor> fetchTutorByUuid(final String uuid) throws BusinessException {

		final Tutor tutor = this.tutorQueryService.fetchTutorByUuid(this.getUserContetx(), uuid);

		return JResponse.ok(tutor).build();
	}

	@Override
	public JResponse<Tutor> resetPassword(final UserContext userContext) throws BusinessException {

		final Tutor tutor = this.tutorQueryService.fetchTutorByEmail(userContext, userContext.getEmail());
		this.tutorService.resetPassword(userContext, tutor);

		return JResponse.ok(tutor).build();
	}

	@Override
	public JResponse<List<TutorLocationDTO>> allocateTutorLocations(final TutorBeanResource tutorBeanResource)
			throws BusinessException {

		final List<TutorLocation> allocateTutorLocations = this.tutorLocationService.allocateTutorLocations(
				tutorBeanResource.getUserContext(), tutorBeanResource.getTutor(), tutorBeanResource.getLocations());

		final List<TutorLocationDTO> tutorLocationsDTO = allocateTutorLocations.stream()
				.map(tutorLocation -> new TutorLocationDTO(tutorLocation)).collect(Collectors.toList());

		return JResponse.ok(tutorLocationsDTO).build();
	}

	@Override
	public JResponse<List<Tutor>> findTutors(String code, String name, String surname, CareerType careerType,
			String phoneNumber, String partnerUuid) throws BusinessException {

		final List<Tutor> tutors = this.tutorQueryService.findTutorsBySelectedFilter(this.getUserContetx(), code, name,
				surname, careerType, phoneNumber, partnerUuid);

		return JResponse.ok(tutors).build();

	}

	@Override
	public JResponse<List<Tutor>> fetchTutorsForUserPartner(String code, String name, String surname,
			CareerType careerType, String phoneNumber, String userUuid) throws BusinessException {

		final Tutor tutor = this.tutorQueryService.fetchTutorByUuid(this.getUserContetx(), userUuid);

		final Partner partner = tutor.getPartner();

		String partnerUuid = partner.getUuid();

		final List<Tutor> tutors = this.tutorQueryService.findTutorsBySelectedFilter(this.getUserContetx(), code, name,
				surname, careerType, StringUtils.isEmpty(phoneNumber) ? null : phoneNumber, partnerUuid);

		return JResponse.ok(tutors).build();
	}

}
