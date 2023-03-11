package seong.onlinestudy.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seong.onlinestudy.domain.Member;
import seong.onlinestudy.domain.Study;
import seong.onlinestudy.dto.StudyDto;
import seong.onlinestudy.exception.DuplicateElementException;
import seong.onlinestudy.repository.StudyRepository;
import seong.onlinestudy.request.StudyCreateRequest;
import seong.onlinestudy.request.StudiesGetRequest;

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

    public Page<StudyDto> getStudies(StudiesGetRequest request, Member loginMember) {
        PageRequest pageRequest = PageRequest.of(request.getPage(), request.getSize());

        Page<StudyDto> studyDtos;
        if(request.getName().isBlank()) { //문자열 검색 조건이 없으면 로그인한 회원의 스터디 목록을 기본으로 가져옴
            LocalDateTime startTime = request.getDate().atStartOfDay().plusHours(5);

            Page<Study> studies = studyRepository.findStudiesByMember(loginMember.getId(), startTime, startTime.plusDays(request.getDays()), pageRequest);
            studyDtos = studies.map(study -> {
                StudyDto studyDto = StudyDto.from(study);
                studyDto.setStudyTime(study);
                return studyDto;
            });
        } else { //있으면 문자열 검색을 통해 스터디 목록을 가져옴
            Page<Study> studies = studyRepository.findAllByNameContains(request.getName(), pageRequest);
            studyDtos = studies.map(StudyDto::from);
        }

        return studyDtos;
    }
}
