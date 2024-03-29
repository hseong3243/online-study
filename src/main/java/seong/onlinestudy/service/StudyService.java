package seong.onlinestudy.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seong.onlinestudy.constant.TimeConst;
import seong.onlinestudy.domain.Study;
import seong.onlinestudy.dto.StudyDto;
import seong.onlinestudy.exception.DuplicateElementException;
import seong.onlinestudy.repository.StudyRepository;
import seong.onlinestudy.request.study.StudyCreateRequest;
import seong.onlinestudy.request.study.StudiesGetRequest;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StudyService {

    private final StudyRepository studyRepository;

    @Transactional
    public Long createStudy(StudyCreateRequest createStudyRequest) {
        studyRepository.findByName(createStudyRequest.getName())
                .ifPresent(study -> {
                    throw new DuplicateElementException("이미 존재하는 스터디입니다.");
                });

        Study study = Study.createStudy(createStudyRequest);
        studyRepository.save(study);

        return study.getId();
    }

    public Page<StudyDto> getStudies(StudiesGetRequest request) {
        PageRequest pageRequest = PageRequest.of(request.getPage(), request.getSize());

        LocalDateTime startTime = null;
        LocalDateTime endTime = null;
        if(request.getDate() != null) {
            startTime = request.getDate().atStartOfDay().plusHours(TimeConst.DAY_START);
            endTime = startTime.plusDays(request.getDays());
        }

        Page<Study> findStudies = studyRepository.findStudies(
                request.getMemberId(),
                request.getGroupId(),
                request.getName(),
                startTime,
                endTime,
                pageRequest);

        Page<StudyDto> studyDtos = findStudies.map(StudyDto::from);

        return studyDtos;
    }
}
