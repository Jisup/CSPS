package com.js.freeproject.domain.problem.application;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.js.freeproject.domain.answer.domain.Answer;
import com.js.freeproject.domain.category.domain.Category;
import com.js.freeproject.domain.category.domain.CategoryRepository;
import com.js.freeproject.domain.problem.domain.AnswerResponse;
import com.js.freeproject.domain.problem.domain.Problem;
import com.js.freeproject.domain.problem.domain.ProblemMakeRequest;
import com.js.freeproject.domain.problem.domain.ProblemRepository;
import com.js.freeproject.domain.problem.domain.ProblemResponse;
import com.js.freeproject.domain.problem.domain.ProblemStatus;
import com.js.freeproject.domain.problem.domain.ProblemStatusResponse;
import com.js.freeproject.domain.user.domain.User;
import com.js.freeproject.domain.user.domain.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProblemService {
    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public Long saveProblem(ProblemMakeRequest problemMakeRequest) {
        User user = userRepository.findById(problemMakeRequest.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));
        Category category = categoryRepository.findById(problemMakeRequest.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("해당 카테고리가 존재하지 않습니다."));
        List<Answer> answers =
                problemMakeRequest.getAnswers().stream().map(Answer::new).collect(toList());
        Problem newProblem = Problem.createProblem(
                problemMakeRequest.getDescription(),
                category,
                user,
                new ArrayList<>(),
                answers);
        try {
            problemRepository.save(newProblem);
        }catch (Exception e){
            throw new IllegalStateException(e.getMessage());
        }
        return newProblem.getId();
    }

    public int findProblems() {
        return problemRepository.findAll().size();
    }

    public Optional<Problem> findProblem(Long id) {
        return problemRepository.findById(id);
    }

    public List<ProblemResponse> findByCategory(Category category, int page) {
        PageRequest pageRequest = PageRequest.of(0, page);
        List<Problem> problems = problemRepository.findByCategory(category.getName(), pageRequest);
        Collections.shuffle(problems);
        return problems.stream().map(ProblemResponse::new).collect(toList());
    }

    public Long findByCategorySize(Category category) {
        PageRequest pageRequest = PageRequest.of(0, 1000);
        List<Problem> problems = problemRepository.findByCategory(category.getName(), pageRequest);
        long size = problems.size();
        return size;
    }

    public List<ProblemStatusResponse> findByStatus(ProblemStatus problemStatus) {
        List<Problem> problems = problemRepository.findByStatus(problemStatus);
        List<ProblemStatusResponse> problemResponses = problems.stream()
                .map(ProblemStatusResponse::new).collect(toList());
        return problemResponses;
    }

    public AnswerResponse findAnswer(Long id, String myAns) {
        Problem problem = problemRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("문제에 답을 찾을 수 없습니다."));
        List<String> proper = new ArrayList<>();
        List<String> wrong = new ArrayList<>();
        extractedAnswer(myAns, problem, proper, wrong);
        AnswerResponse answerResponse = new AnswerResponse(proper, wrong);
        return answerResponse;
    }

    private void extractedAnswer(String myAns, Problem problem, List<String> proper, List<String> wrong) {
        for (Answer answer : problem.getAnswers()) {
            if (myAns.contains(answer.getWord())) {
                proper.add(answer.getWord());
            } else {
                wrong.add(answer.getWord());
            }
        }
    }

    @Transactional
    public String updateStatus(Long id, ProblemStatus problemStatus) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("문제가 존재하지 않습니다."));

        problem.setStatus(problemStatus);
        return "변경이 완료되었습니다.";
    }
}
