package study.querydsl.member.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import study.querydsl.member.Member;
import study.querydsl.member.dto.MemberSearchCondition;
import study.querydsl.member.dto.MemberTeamDto;
import study.querydsl.member.dto.QMemberTeamDto;
import study.querydsl.member.dto.UserDto;

import static org.springframework.util.StringUtils.hasText;
import static org.springframework.util.StringUtils.isEmpty;
import static study.querydsl.member.QMember.member;
import static study.querydsl.team.QTeam.team;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import java.util.List;
import java.util.Optional;

@Repository
public class MemberJpaRepository {
    private final EntityManager em; //jpa 접근 시 필요
    private final JPAQueryFactory queryFactory; //querydsl 사용 시 필요

    public MemberJpaRepository(EntityManager em) {
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);
    }

    public void save(Member member) {
        em.persist(member);
    }

    public Optional<Member> findById(Long id) {
        Member findMember = em.find(Member.class, id);
        return Optional.ofNullable(findMember);
    }

    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }

    public List<Member> findAll_querydsl() {
        return queryFactory
                .selectFrom(member)
                .fetch();

    }

    public List<Member> findByUsername(String username) {
        List<Member> findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", username)
                .getResultList();
        return findMember;
    }

    public List<Member> findByUsername_querydsl(String username) {
        return queryFactory
                .selectFrom(member)
                .where(member.username.eq(username))
                .fetch();
    }

    //Bulider 이용
    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition) {
        BooleanBuilder builder = new BooleanBuilder();

        if(hasText(condition.getUsername())) {
            builder.and(member.username.eq(condition.getUsername()));
        }
        if (hasText(condition.getTeamName())) {
            builder.and(team.name.eq(condition.getTeamName()));
        }
        if (condition.getAgeGoe() != null) {
            builder.and(member.age.goe(condition.getAgeGoe()));
        }
        if (condition.getAgeLoe() != null) {
            builder.and(member.age.loe(condition.getAgeLoe()));
        }

        return queryFactory
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id,
                        team.name))
                .from(member)
                .leftJoin(member.team, team)
                .where(builder)
                .limit(100)
                .fetch();
    }

    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id,
                        team.name))
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageBetween(condition.getAgeGoe(), condition.getAgeLoe()))
                .limit(100)
                .fetch();
    }

    private BooleanExpression usernameEq(String username) {
        return isEmpty(username) ? null : member.username.eq(username);
    }

    private BooleanExpression teamNameEq(String teamName) {
        return isEmpty(teamName) ? null : team.name.eq(teamName);
    }

    private BooleanExpression ageBetween(Integer ageGoe, Integer ageLoe) {
        if (ageLoe(ageLoe) == null && ageGoe(ageGoe) == null) {
            return null;
        } else {
            return ageLoe(ageLoe).and(ageGoe(ageGoe));
        }
    }
    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe == null ? null : member.age.goe(ageGoe);
    }
    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe == null ? null : member.age.loe(ageLoe);
    }

}
