--
-- PostgreSQL database dump
--

-- Dumped from database version 14.17 (Homebrew)
-- Dumped by pg_dump version 14.17 (Homebrew)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: aggregate_id_generator; Type: TABLE; Schema: public; Owner: tomasnascimento
--

CREATE TABLE public.aggregate_id_generator (
    id integer NOT NULL
);


ALTER TABLE public.aggregate_id_generator OWNER TO tomasnascimento;

--
-- Name: aggregate_id_generator_seq; Type: SEQUENCE; Schema: public; Owner: tomasnascimento
--

CREATE SEQUENCE public.aggregate_id_generator_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.aggregate_id_generator_seq OWNER TO tomasnascimento;

--
-- Name: aggregate_seq; Type: SEQUENCE; Schema: public; Owner: tomasnascimento
--

CREATE SEQUENCE public.aggregate_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.aggregate_seq OWNER TO tomasnascimento;

--
-- Name: answer_course_execution; Type: TABLE; Schema: public; Owner: tomasnascimento
--

CREATE TABLE public.answer_course_execution (
    id bigint NOT NULL,
    course_execution_aggregate_id integer,
    course_execution_version integer,
    quiz_answer_id integer
);


ALTER TABLE public.answer_course_execution OWNER TO tomasnascimento;

--
-- Name: answer_course_execution_seq; Type: SEQUENCE; Schema: public; Owner: tomasnascimento
--

CREATE SEQUENCE public.answer_course_execution_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.answer_course_execution_seq OWNER TO tomasnascimento;

--
-- Name: answer_student; Type: TABLE; Schema: public; Owner: tomasnascimento
--

CREATE TABLE public.answer_student (
    id bigint NOT NULL,
    name character varying(255),
    student_aggregate_id integer,
    student_state smallint,
    quiz_answer_id integer,
    CONSTRAINT answer_student_student_state_check CHECK (((student_state >= 0) AND (student_state <= 2)))
);


ALTER TABLE public.answer_student OWNER TO tomasnascimento;

--
-- Name: answer_student_seq; Type: SEQUENCE; Schema: public; Owner: tomasnascimento
--

CREATE SEQUENCE public.answer_student_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.answer_student_seq OWNER TO tomasnascimento;

--
-- Name: answered_quiz; Type: TABLE; Schema: public; Owner: tomasnascimento
--

CREATE TABLE public.answered_quiz (
    id bigint NOT NULL,
    quiz_aggregate_id integer,
    quiz_version integer,
    quiz_answer_id integer
);


ALTER TABLE public.answered_quiz OWNER TO tomasnascimento;

--
-- Name: answered_quiz_quiz_questions_aggregate_ids; Type: TABLE; Schema: public; Owner: tomasnascimento
--

CREATE TABLE public.answered_quiz_quiz_questions_aggregate_ids (
    answered_quiz_id bigint NOT NULL,
    quiz_questions_aggregate_ids integer
);


ALTER TABLE public.answered_quiz_quiz_questions_aggregate_ids OWNER TO tomasnascimento;

--
-- Name: answered_quiz_seq; Type: SEQUENCE; Schema: public; Owner: tomasnascimento
--

CREATE SEQUENCE public.answered_quiz_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.answered_quiz_seq OWNER TO tomasnascimento;

--
-- Name: causal_course; Type: TABLE; Schema: public; Owner: tomasnascimento
--

CREATE TABLE public.causal_course (
    id integer NOT NULL,
    aggregate_id integer,
    aggregate_type character varying(255),
    creation_ts timestamp(6) without time zone,
    state character varying(255),
    version integer,
    prev_id integer,
    name character varying(255),
    type character varying(255),
    CONSTRAINT causal_course_state_check CHECK (((state)::text = ANY (ARRAY[('ACTIVE'::character varying)::text, ('INACTIVE'::character varying)::text, ('DELETED'::character varying)::text]))),
    CONSTRAINT causal_course_type_check CHECK (((type)::text = ANY (ARRAY[('TECNICO'::character varying)::text, ('EXTERNAL'::character varying)::text])))
);


ALTER TABLE public.causal_course OWNER TO tomasnascimento;

--
-- Name: causal_course_execution; Type: TABLE; Schema: public; Owner: tomasnascimento
--

CREATE TABLE public.causal_course_execution (
    id integer NOT NULL,
    aggregate_id integer,
    aggregate_type character varying(255),
    creation_ts timestamp(6) without time zone,
    state character varying(255),
    version integer,
    prev_id integer,
    academic_term character varying(255),
    acronym character varying(255),
    end_date timestamp(6) without time zone,
    CONSTRAINT causal_course_execution_state_check CHECK (((state)::text = ANY (ARRAY[('ACTIVE'::character varying)::text, ('INACTIVE'::character varying)::text, ('DELETED'::character varying)::text])))
);


ALTER TABLE public.causal_course_execution OWNER TO tomasnascimento;

--
-- Name: causal_question; Type: TABLE; Schema: public; Owner: tomasnascimento
--

CREATE TABLE public.causal_question (
    id integer NOT NULL,
    aggregate_id integer,
    aggregate_type character varying(255),
    creation_ts timestamp(6) without time zone,
    state character varying(255),
    version integer,
    prev_id integer,
    content character varying(255),
    creation_date timestamp(6) without time zone,
    title character varying(255),
    CONSTRAINT causal_question_state_check CHECK (((state)::text = ANY (ARRAY[('ACTIVE'::character varying)::text, ('INACTIVE'::character varying)::text, ('DELETED'::character varying)::text])))
);


ALTER TABLE public.causal_question OWNER TO tomasnascimento;

--
-- Name: causal_quiz; Type: TABLE; Schema: public; Owner: tomasnascimento
--

CREATE TABLE public.causal_quiz (
    id integer NOT NULL,
    aggregate_id integer,
    aggregate_type character varying(255),
    creation_ts timestamp(6) without time zone,
    state character varying(255),
    version integer,
    prev_id integer,
    available_date timestamp(6) without time zone,
    conclusion_date timestamp(6) without time zone,
    creation_date timestamp(6) without time zone,
    quiz_type character varying(255),
    results_date timestamp(6) without time zone,
    title character varying(255),
    CONSTRAINT causal_quiz_quiz_type_check CHECK (((quiz_type)::text = ANY (ARRAY[('EXAM'::character varying)::text, ('TEST'::character varying)::text, ('GENERATED'::character varying)::text, ('PROPOSED'::character varying)::text, ('IN_CLASS'::character varying)::text, ('EXTERNAL_QUIZ'::character varying)::text]))),
    CONSTRAINT causal_quiz_state_check CHECK (((state)::text = ANY (ARRAY[('ACTIVE'::character varying)::text, ('INACTIVE'::character varying)::text, ('DELETED'::character varying)::text])))
);


ALTER TABLE public.causal_quiz OWNER TO tomasnascimento;

--
-- Name: causal_quiz_answer; Type: TABLE; Schema: public; Owner: tomasnascimento
--

CREATE TABLE public.causal_quiz_answer (
    id integer NOT NULL,
    aggregate_id integer,
    aggregate_type character varying(255),
    creation_ts timestamp(6) without time zone,
    state character varying(255),
    version integer,
    prev_id integer,
    answer_date timestamp(6) without time zone,
    completed boolean NOT NULL,
    creation_date timestamp(6) without time zone,
    CONSTRAINT causal_quiz_answer_state_check CHECK (((state)::text = ANY (ARRAY[('ACTIVE'::character varying)::text, ('INACTIVE'::character varying)::text, ('DELETED'::character varying)::text])))
);


ALTER TABLE public.causal_quiz_answer OWNER TO tomasnascimento;

--
-- Name: causal_topic; Type: TABLE; Schema: public; Owner: tomasnascimento
--

CREATE TABLE public.causal_topic (
    id integer NOT NULL,
    aggregate_id integer,
    aggregate_type character varying(255),
    creation_ts timestamp(6) without time zone,
    state character varying(255),
    version integer,
    prev_id integer,
    name character varying(255),
    CONSTRAINT causal_topic_state_check CHECK (((state)::text = ANY (ARRAY[('ACTIVE'::character varying)::text, ('INACTIVE'::character varying)::text, ('DELETED'::character varying)::text])))
);


ALTER TABLE public.causal_topic OWNER TO tomasnascimento;

--
-- Name: causal_tournament; Type: TABLE; Schema: public; Owner: tomasnascimento
--

CREATE TABLE public.causal_tournament (
    id integer NOT NULL,
    aggregate_id integer,
    aggregate_type character varying(255),
    creation_ts timestamp(6) without time zone,
    state character varying(255),
    version integer,
    prev_id integer,
    cancelled boolean NOT NULL,
    end_time timestamp(6) without time zone,
    number_of_questions integer,
    start_time timestamp(6) without time zone,
    CONSTRAINT causal_tournament_state_check CHECK (((state)::text = ANY (ARRAY[('ACTIVE'::character varying)::text, ('INACTIVE'::character varying)::text, ('DELETED'::character varying)::text])))
);


ALTER TABLE public.causal_tournament OWNER TO tomasnascimento;

--
-- Name: causal_user; Type: TABLE; Schema: public; Owner: tomasnascimento
--

CREATE TABLE public.causal_user (
    id integer NOT NULL,
    aggregate_id integer,
    aggregate_type character varying(255),
    creation_ts timestamp(6) without time zone,
    state character varying(255),
    version integer,
    prev_id integer,
    active boolean DEFAULT false,
    name character varying(255),
    role character varying(255),
    username character varying(255),
    CONSTRAINT causal_user_role_check CHECK (((role)::text = ANY (ARRAY[('STUDENT'::character varying)::text, ('TEACHER'::character varying)::text, ('ADMIN'::character varying)::text]))),
    CONSTRAINT causal_user_state_check CHECK (((state)::text = ANY (ARRAY[('ACTIVE'::character varying)::text, ('INACTIVE'::character varying)::text, ('DELETED'::character varying)::text])))
);


ALTER TABLE public.causal_user OWNER TO tomasnascimento;

--
-- Name: course_execution_course; Type: TABLE; Schema: public; Owner: tomasnascimento
--

CREATE TABLE public.course_execution_course (
    id bigint NOT NULL,
    course_aggregate_id integer,
    course_version integer,
    name character varying(255),
    type character varying(255),
    course_execution_id integer,
    CONSTRAINT course_execution_course_type_check CHECK (((type)::text = ANY (ARRAY[('TECNICO'::character varying)::text, ('EXTERNAL'::character varying)::text])))
);


ALTER TABLE public.course_execution_course OWNER TO tomasnascimento;

--
-- Name: course_execution_course_seq; Type: SEQUENCE; Schema: public; Owner: tomasnascimento
--

CREATE SEQUENCE public.course_execution_course_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.course_execution_course_seq OWNER TO tomasnascimento;

--
-- Name: course_execution_student; Type: TABLE; Schema: public; Owner: tomasnascimento
--

CREATE TABLE public.course_execution_student (
    id bigint NOT NULL,
    active boolean NOT NULL,
    name character varying(255),
    state smallint,
    user_aggregate_id integer,
    user_version integer,
    username character varying(255),
    course_execution_id integer,
    CONSTRAINT course_execution_student_state_check CHECK (((state >= 0) AND (state <= 2)))
);


ALTER TABLE public.course_execution_student OWNER TO tomasnascimento;

--
-- Name: course_execution_student_seq; Type: SEQUENCE; Schema: public; Owner: tomasnascimento
--

CREATE SEQUENCE public.course_execution_student_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.course_execution_student_seq OWNER TO tomasnascimento;

--
-- Name: event; Type: TABLE; Schema: public; Owner: tomasnascimento
--

CREATE TABLE public.event (
    dtype character varying(31) NOT NULL,
    id integer NOT NULL,
    publisher_aggregate_id integer,
    publisher_aggregate_version integer,
    "timestamp" timestamp(6) without time zone,
    correct boolean,
    question_aggregate_id integer,
    quiz_aggregate_id integer,
    student_aggregate_id integer,
    name character varying(255),
    username character varying(255),
    updated_name character varying(255),
    content character varying(255),
    title character varying(255),
    topic_name character varying(255)
);


ALTER TABLE public.event OWNER TO tomasnascimento;

--
-- Name: event_seq; Type: SEQUENCE; Schema: public; Owner: tomasnascimento
--

CREATE SEQUENCE public.event_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.event_seq OWNER TO tomasnascimento;

--
-- Name: option; Type: TABLE; Schema: public; Owner: tomasnascimento
--

CREATE TABLE public.option (
    id bigint NOT NULL,
    content character varying(255),
    correct boolean NOT NULL,
    option_key integer,
    sequence integer,
    question_id integer
);


ALTER TABLE public.option OWNER TO tomasnascimento;

--
-- Name: option_seq; Type: SEQUENCE; Schema: public; Owner: tomasnascimento
--

CREATE SEQUENCE public.option_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.option_seq OWNER TO tomasnascimento;

--
-- Name: question_answer; Type: TABLE; Schema: public; Owner: tomasnascimento
--

CREATE TABLE public.question_answer (
    id bigint NOT NULL,
    correct boolean NOT NULL,
    option_key integer,
    option_sequence_choice integer,
    question_aggregate_id integer,
    question_version integer,
    state smallint,
    time_taken integer,
    quiz_answer_id integer,
    CONSTRAINT question_answer_state_check CHECK (((state >= 0) AND (state <= 2)))
);


ALTER TABLE public.question_answer OWNER TO tomasnascimento;

--
-- Name: question_answer_seq; Type: SEQUENCE; Schema: public; Owner: tomasnascimento
--

CREATE SEQUENCE public.question_answer_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.question_answer_seq OWNER TO tomasnascimento;

--
-- Name: question_course; Type: TABLE; Schema: public; Owner: tomasnascimento
--

CREATE TABLE public.question_course (
    id bigint NOT NULL,
    course_aggregate_id integer,
    course_name character varying(255),
    course_version integer,
    question_id integer
);


ALTER TABLE public.question_course OWNER TO tomasnascimento;

--
-- Name: question_course_seq; Type: SEQUENCE; Schema: public; Owner: tomasnascimento
--

CREATE SEQUENCE public.question_course_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.question_course_seq OWNER TO tomasnascimento;

--
-- Name: question_topic; Type: TABLE; Schema: public; Owner: tomasnascimento
--

CREATE TABLE public.question_topic (
    id bigint NOT NULL,
    state smallint,
    topic_aggregate_id integer,
    topic_name character varying(255),
    topic_version integer,
    question_id integer,
    CONSTRAINT question_topic_state_check CHECK (((state >= 0) AND (state <= 2)))
);


ALTER TABLE public.question_topic OWNER TO tomasnascimento;

--
-- Name: question_topic_seq; Type: SEQUENCE; Schema: public; Owner: tomasnascimento
--

CREATE SEQUENCE public.question_topic_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.question_topic_seq OWNER TO tomasnascimento;

--
-- Name: quiz_course_execution; Type: TABLE; Schema: public; Owner: tomasnascimento
--

CREATE TABLE public.quiz_course_execution (
    id bigint NOT NULL,
    course_execution_aggregate_id integer,
    course_execution_version integer,
    quiz_id integer
);


ALTER TABLE public.quiz_course_execution OWNER TO tomasnascimento;

--
-- Name: quiz_course_execution_seq; Type: SEQUENCE; Schema: public; Owner: tomasnascimento
--

CREATE SEQUENCE public.quiz_course_execution_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.quiz_course_execution_seq OWNER TO tomasnascimento;

--
-- Name: quiz_question; Type: TABLE; Schema: public; Owner: tomasnascimento
--

CREATE TABLE public.quiz_question (
    id bigint NOT NULL,
    content character varying(255),
    question_aggregate_id integer,
    question_version integer,
    sequence integer,
    state smallint,
    title character varying(255),
    quiz_id integer,
    CONSTRAINT quiz_question_state_check CHECK (((state >= 0) AND (state <= 2)))
);


ALTER TABLE public.quiz_question OWNER TO tomasnascimento;

--
-- Name: quiz_question_seq; Type: SEQUENCE; Schema: public; Owner: tomasnascimento
--

CREATE SEQUENCE public.quiz_question_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.quiz_question_seq OWNER TO tomasnascimento;

--
-- Name: saga_course; Type: TABLE; Schema: public; Owner: tomasnascimento
--

CREATE TABLE public.saga_course (
    id integer NOT NULL,
    aggregate_id integer,
    aggregate_type character varying(255),
    creation_ts timestamp(6) without time zone,
    state character varying(255),
    version integer,
    prev_id integer,
    name character varying(255),
    type character varying(255),
    saga_state character varying(255),
    CONSTRAINT saga_course_state_check CHECK (((state)::text = ANY (ARRAY[('ACTIVE'::character varying)::text, ('INACTIVE'::character varying)::text, ('DELETED'::character varying)::text]))),
    CONSTRAINT saga_course_type_check CHECK (((type)::text = ANY (ARRAY[('TECNICO'::character varying)::text, ('EXTERNAL'::character varying)::text])))
);


ALTER TABLE public.saga_course OWNER TO tomasnascimento;

--
-- Name: saga_course_execution; Type: TABLE; Schema: public; Owner: tomasnascimento
--

CREATE TABLE public.saga_course_execution (
    id integer NOT NULL,
    aggregate_id integer,
    aggregate_type character varying(255),
    creation_ts timestamp(6) without time zone,
    state character varying(255),
    version integer,
    prev_id integer,
    academic_term character varying(255),
    acronym character varying(255),
    end_date timestamp(6) without time zone,
    saga_state character varying(255),
    CONSTRAINT saga_course_execution_state_check CHECK (((state)::text = ANY (ARRAY[('ACTIVE'::character varying)::text, ('INACTIVE'::character varying)::text, ('DELETED'::character varying)::text])))
);


ALTER TABLE public.saga_course_execution OWNER TO tomasnascimento;

--
-- Name: saga_question; Type: TABLE; Schema: public; Owner: tomasnascimento
--

CREATE TABLE public.saga_question (
    id integer NOT NULL,
    aggregate_id integer,
    aggregate_type character varying(255),
    creation_ts timestamp(6) without time zone,
    state character varying(255),
    version integer,
    prev_id integer,
    content character varying(255),
    creation_date timestamp(6) without time zone,
    title character varying(255),
    saga_state character varying(255),
    CONSTRAINT saga_question_state_check CHECK (((state)::text = ANY (ARRAY[('ACTIVE'::character varying)::text, ('INACTIVE'::character varying)::text, ('DELETED'::character varying)::text])))
);


ALTER TABLE public.saga_question OWNER TO tomasnascimento;

--
-- Name: saga_quiz; Type: TABLE; Schema: public; Owner: tomasnascimento
--

CREATE TABLE public.saga_quiz (
    id integer NOT NULL,
    aggregate_id integer,
    aggregate_type character varying(255),
    creation_ts timestamp(6) without time zone,
    state character varying(255),
    version integer,
    prev_id integer,
    available_date timestamp(6) without time zone,
    conclusion_date timestamp(6) without time zone,
    creation_date timestamp(6) without time zone,
    quiz_type character varying(255),
    results_date timestamp(6) without time zone,
    title character varying(255),
    saga_state character varying(255),
    CONSTRAINT saga_quiz_quiz_type_check CHECK (((quiz_type)::text = ANY (ARRAY[('EXAM'::character varying)::text, ('TEST'::character varying)::text, ('GENERATED'::character varying)::text, ('PROPOSED'::character varying)::text, ('IN_CLASS'::character varying)::text, ('EXTERNAL_QUIZ'::character varying)::text]))),
    CONSTRAINT saga_quiz_state_check CHECK (((state)::text = ANY (ARRAY[('ACTIVE'::character varying)::text, ('INACTIVE'::character varying)::text, ('DELETED'::character varying)::text])))
);


ALTER TABLE public.saga_quiz OWNER TO tomasnascimento;

--
-- Name: saga_quiz_answer; Type: TABLE; Schema: public; Owner: tomasnascimento
--

CREATE TABLE public.saga_quiz_answer (
    id integer NOT NULL,
    aggregate_id integer,
    aggregate_type character varying(255),
    creation_ts timestamp(6) without time zone,
    state character varying(255),
    version integer,
    prev_id integer,
    answer_date timestamp(6) without time zone,
    completed boolean NOT NULL,
    creation_date timestamp(6) without time zone,
    saga_state character varying(255),
    CONSTRAINT saga_quiz_answer_state_check CHECK (((state)::text = ANY (ARRAY[('ACTIVE'::character varying)::text, ('INACTIVE'::character varying)::text, ('DELETED'::character varying)::text])))
);


ALTER TABLE public.saga_quiz_answer OWNER TO tomasnascimento;

--
-- Name: saga_topic; Type: TABLE; Schema: public; Owner: tomasnascimento
--

CREATE TABLE public.saga_topic (
    id integer NOT NULL,
    aggregate_id integer,
    aggregate_type character varying(255),
    creation_ts timestamp(6) without time zone,
    state character varying(255),
    version integer,
    prev_id integer,
    name character varying(255),
    saga_state character varying(255),
    CONSTRAINT saga_topic_state_check CHECK (((state)::text = ANY (ARRAY[('ACTIVE'::character varying)::text, ('INACTIVE'::character varying)::text, ('DELETED'::character varying)::text])))
);


ALTER TABLE public.saga_topic OWNER TO tomasnascimento;

--
-- Name: saga_tournament; Type: TABLE; Schema: public; Owner: tomasnascimento
--

CREATE TABLE public.saga_tournament (
    id integer NOT NULL,
    aggregate_id integer,
    aggregate_type character varying(255),
    creation_ts timestamp(6) without time zone,
    state character varying(255),
    version integer,
    prev_id integer,
    cancelled boolean NOT NULL,
    end_time timestamp(6) without time zone,
    number_of_questions integer,
    start_time timestamp(6) without time zone,
    saga_state character varying(255),
    CONSTRAINT saga_tournament_state_check CHECK (((state)::text = ANY (ARRAY[('ACTIVE'::character varying)::text, ('INACTIVE'::character varying)::text, ('DELETED'::character varying)::text])))
);


ALTER TABLE public.saga_tournament OWNER TO tomasnascimento;

--
-- Name: saga_user; Type: TABLE; Schema: public; Owner: tomasnascimento
--

CREATE TABLE public.saga_user (
    id integer NOT NULL,
    aggregate_id integer,
    aggregate_type character varying(255),
    creation_ts timestamp(6) without time zone,
    state character varying(255),
    version integer,
    prev_id integer,
    active boolean DEFAULT false,
    name character varying(255),
    role character varying(255),
    username character varying(255),
    saga_state character varying(255),
    CONSTRAINT saga_user_role_check CHECK (((role)::text = ANY (ARRAY[('STUDENT'::character varying)::text, ('TEACHER'::character varying)::text, ('ADMIN'::character varying)::text]))),
    CONSTRAINT saga_user_state_check CHECK (((state)::text = ANY (ARRAY[('ACTIVE'::character varying)::text, ('INACTIVE'::character varying)::text, ('DELETED'::character varying)::text])))
);


ALTER TABLE public.saga_user OWNER TO tomasnascimento;

--
-- Name: topic_course; Type: TABLE; Schema: public; Owner: tomasnascimento
--

CREATE TABLE public.topic_course (
    id integer NOT NULL,
    course_aggregate_id integer,
    course_version integer,
    topic_id integer
);


ALTER TABLE public.topic_course OWNER TO tomasnascimento;

--
-- Name: topic_course_seq; Type: SEQUENCE; Schema: public; Owner: tomasnascimento
--

CREATE SEQUENCE public.topic_course_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.topic_course_seq OWNER TO tomasnascimento;

--
-- Name: tournament_course_execution; Type: TABLE; Schema: public; Owner: tomasnascimento
--

CREATE TABLE public.tournament_course_execution (
    id bigint NOT NULL,
    course_execution_acronym character varying(255),
    course_execution_aggregate_id integer,
    course_execution_course_id integer,
    course_execution_status character varying(255),
    course_execution_version integer,
    tournament_id integer
);


ALTER TABLE public.tournament_course_execution OWNER TO tomasnascimento;

--
-- Name: tournament_course_execution_seq; Type: SEQUENCE; Schema: public; Owner: tomasnascimento
--

CREATE SEQUENCE public.tournament_course_execution_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.tournament_course_execution_seq OWNER TO tomasnascimento;

--
-- Name: tournament_creator; Type: TABLE; Schema: public; Owner: tomasnascimento
--

CREATE TABLE public.tournament_creator (
    id bigint NOT NULL,
    creator_aggregate_id integer,
    creator_name character varying(255),
    creator_state smallint,
    creator_username character varying(255),
    creator_version integer,
    tournament_id integer,
    CONSTRAINT tournament_creator_creator_state_check CHECK (((creator_state >= 0) AND (creator_state <= 2)))
);


ALTER TABLE public.tournament_creator OWNER TO tomasnascimento;

--
-- Name: tournament_creator_seq; Type: SEQUENCE; Schema: public; Owner: tomasnascimento
--

CREATE SEQUENCE public.tournament_creator_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.tournament_creator_seq OWNER TO tomasnascimento;

--
-- Name: tournament_participant; Type: TABLE; Schema: public; Owner: tomasnascimento
--

CREATE TABLE public.tournament_participant (
    id bigint NOT NULL,
    enroll_time timestamp(6) without time zone,
    participant_aggregate_id integer,
    participant_name character varying(255),
    participant_username character varying(255),
    participant_version integer,
    state character varying(255),
    tournament_id integer,
    CONSTRAINT tournament_participant_state_check CHECK (((state)::text = ANY (ARRAY[('ACTIVE'::character varying)::text, ('INACTIVE'::character varying)::text, ('DELETED'::character varying)::text])))
);


ALTER TABLE public.tournament_participant OWNER TO tomasnascimento;

--
-- Name: tournament_participant_quiz_answer; Type: TABLE; Schema: public; Owner: tomasnascimento
--

CREATE TABLE public.tournament_participant_quiz_answer (
    id bigint NOT NULL,
    answered boolean NOT NULL,
    number_of_answered integer,
    number_of_correct integer,
    quiz_answer_aggregate_id integer,
    quiz_answer_version integer,
    tournament_participant_id bigint
);


ALTER TABLE public.tournament_participant_quiz_answer OWNER TO tomasnascimento;

--
-- Name: tournament_participant_quiz_answer_seq; Type: SEQUENCE; Schema: public; Owner: tomasnascimento
--

CREATE SEQUENCE public.tournament_participant_quiz_answer_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.tournament_participant_quiz_answer_seq OWNER TO tomasnascimento;

--
-- Name: tournament_participant_seq; Type: SEQUENCE; Schema: public; Owner: tomasnascimento
--

CREATE SEQUENCE public.tournament_participant_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.tournament_participant_seq OWNER TO tomasnascimento;

--
-- Name: tournament_quiz; Type: TABLE; Schema: public; Owner: tomasnascimento
--

CREATE TABLE public.tournament_quiz (
    id bigint NOT NULL,
    quiz_aggregate_id integer,
    quiz_version integer,
    tournament_id integer
);


ALTER TABLE public.tournament_quiz OWNER TO tomasnascimento;

--
-- Name: tournament_quiz_seq; Type: SEQUENCE; Schema: public; Owner: tomasnascimento
--

CREATE SEQUENCE public.tournament_quiz_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.tournament_quiz_seq OWNER TO tomasnascimento;

--
-- Name: tournament_topic; Type: TABLE; Schema: public; Owner: tomasnascimento
--

CREATE TABLE public.tournament_topic (
    id bigint NOT NULL,
    state character varying(255),
    topic_aggregate_id integer,
    topic_course_aggregate_id integer,
    topic_name character varying(255),
    topic_version integer,
    tournament_id integer,
    CONSTRAINT tournament_topic_state_check CHECK (((state)::text = ANY (ARRAY[('ACTIVE'::character varying)::text, ('INACTIVE'::character varying)::text, ('DELETED'::character varying)::text])))
);


ALTER TABLE public.tournament_topic OWNER TO tomasnascimento;

--
-- Name: tournament_topic_seq; Type: SEQUENCE; Schema: public; Owner: tomasnascimento
--

CREATE SEQUENCE public.tournament_topic_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.tournament_topic_seq OWNER TO tomasnascimento;

--
-- Name: version; Type: TABLE; Schema: public; Owner: tomasnascimento
--

CREATE TABLE public.version (
    id integer NOT NULL,
    number_of_decrements integer,
    version_number integer
);


ALTER TABLE public.version OWNER TO tomasnascimento;

--
-- Name: version_seq; Type: SEQUENCE; Schema: public; Owner: tomasnascimento
--

CREATE SEQUENCE public.version_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.version_seq OWNER TO tomasnascimento;

--
-- Data for Name: aggregate_id_generator; Type: TABLE DATA; Schema: public; Owner: tomasnascimento
--

COPY public.aggregate_id_generator (id) FROM stdin;
\.


--
-- Data for Name: answer_course_execution; Type: TABLE DATA; Schema: public; Owner: tomasnascimento
--

COPY public.answer_course_execution (id, course_execution_aggregate_id, course_execution_version, quiz_answer_id) FROM stdin;
\.


--
-- Data for Name: answer_student; Type: TABLE DATA; Schema: public; Owner: tomasnascimento
--

COPY public.answer_student (id, name, student_aggregate_id, student_state, quiz_answer_id) FROM stdin;
\.


--
-- Data for Name: answered_quiz; Type: TABLE DATA; Schema: public; Owner: tomasnascimento
--

COPY public.answered_quiz (id, quiz_aggregate_id, quiz_version, quiz_answer_id) FROM stdin;
\.


--
-- Data for Name: answered_quiz_quiz_questions_aggregate_ids; Type: TABLE DATA; Schema: public; Owner: tomasnascimento
--

COPY public.answered_quiz_quiz_questions_aggregate_ids (answered_quiz_id, quiz_questions_aggregate_ids) FROM stdin;
\.


--
-- Data for Name: causal_course; Type: TABLE DATA; Schema: public; Owner: tomasnascimento
--

COPY public.causal_course (id, aggregate_id, aggregate_type, creation_ts, state, version, prev_id, name, type) FROM stdin;
\.


--
-- Data for Name: causal_course_execution; Type: TABLE DATA; Schema: public; Owner: tomasnascimento
--

COPY public.causal_course_execution (id, aggregate_id, aggregate_type, creation_ts, state, version, prev_id, academic_term, acronym, end_date) FROM stdin;
\.


--
-- Data for Name: causal_question; Type: TABLE DATA; Schema: public; Owner: tomasnascimento
--

COPY public.causal_question (id, aggregate_id, aggregate_type, creation_ts, state, version, prev_id, content, creation_date, title) FROM stdin;
\.


--
-- Data for Name: causal_quiz; Type: TABLE DATA; Schema: public; Owner: tomasnascimento
--

COPY public.causal_quiz (id, aggregate_id, aggregate_type, creation_ts, state, version, prev_id, available_date, conclusion_date, creation_date, quiz_type, results_date, title) FROM stdin;
\.


--
-- Data for Name: causal_quiz_answer; Type: TABLE DATA; Schema: public; Owner: tomasnascimento
--

COPY public.causal_quiz_answer (id, aggregate_id, aggregate_type, creation_ts, state, version, prev_id, answer_date, completed, creation_date) FROM stdin;
\.


--
-- Data for Name: causal_topic; Type: TABLE DATA; Schema: public; Owner: tomasnascimento
--

COPY public.causal_topic (id, aggregate_id, aggregate_type, creation_ts, state, version, prev_id, name) FROM stdin;
\.


--
-- Data for Name: causal_tournament; Type: TABLE DATA; Schema: public; Owner: tomasnascimento
--

COPY public.causal_tournament (id, aggregate_id, aggregate_type, creation_ts, state, version, prev_id, cancelled, end_time, number_of_questions, start_time) FROM stdin;
\.


--
-- Data for Name: causal_user; Type: TABLE DATA; Schema: public; Owner: tomasnascimento
--

COPY public.causal_user (id, aggregate_id, aggregate_type, creation_ts, state, version, prev_id, active, name, role, username) FROM stdin;
\.


--
-- Data for Name: course_execution_course; Type: TABLE DATA; Schema: public; Owner: tomasnascimento
--

COPY public.course_execution_course (id, course_aggregate_id, course_version, name, type, course_execution_id) FROM stdin;
\.


--
-- Data for Name: course_execution_student; Type: TABLE DATA; Schema: public; Owner: tomasnascimento
--

COPY public.course_execution_student (id, active, name, state, user_aggregate_id, user_version, username, course_execution_id) FROM stdin;
\.


--
-- Data for Name: event; Type: TABLE DATA; Schema: public; Owner: tomasnascimento
--

COPY public.event (dtype, id, publisher_aggregate_id, publisher_aggregate_version, "timestamp", correct, question_aggregate_id, quiz_aggregate_id, student_aggregate_id, name, username, updated_name, content, title, topic_name) FROM stdin;
\.


--
-- Data for Name: option; Type: TABLE DATA; Schema: public; Owner: tomasnascimento
--

COPY public.option (id, content, correct, option_key, sequence, question_id) FROM stdin;
\.


--
-- Data for Name: question_answer; Type: TABLE DATA; Schema: public; Owner: tomasnascimento
--

COPY public.question_answer (id, correct, option_key, option_sequence_choice, question_aggregate_id, question_version, state, time_taken, quiz_answer_id) FROM stdin;
\.


--
-- Data for Name: question_course; Type: TABLE DATA; Schema: public; Owner: tomasnascimento
--

COPY public.question_course (id, course_aggregate_id, course_name, course_version, question_id) FROM stdin;
\.


--
-- Data for Name: question_topic; Type: TABLE DATA; Schema: public; Owner: tomasnascimento
--

COPY public.question_topic (id, state, topic_aggregate_id, topic_name, topic_version, question_id) FROM stdin;
\.


--
-- Data for Name: quiz_course_execution; Type: TABLE DATA; Schema: public; Owner: tomasnascimento
--

COPY public.quiz_course_execution (id, course_execution_aggregate_id, course_execution_version, quiz_id) FROM stdin;
\.


--
-- Data for Name: quiz_question; Type: TABLE DATA; Schema: public; Owner: tomasnascimento
--

COPY public.quiz_question (id, content, question_aggregate_id, question_version, sequence, state, title, quiz_id) FROM stdin;
\.


--
-- Data for Name: saga_course; Type: TABLE DATA; Schema: public; Owner: tomasnascimento
--

COPY public.saga_course (id, aggregate_id, aggregate_type, creation_ts, state, version, prev_id, name, type, saga_state) FROM stdin;
\.


--
-- Data for Name: saga_course_execution; Type: TABLE DATA; Schema: public; Owner: tomasnascimento
--

COPY public.saga_course_execution (id, aggregate_id, aggregate_type, creation_ts, state, version, prev_id, academic_term, acronym, end_date, saga_state) FROM stdin;
\.


--
-- Data for Name: saga_question; Type: TABLE DATA; Schema: public; Owner: tomasnascimento
--

COPY public.saga_question (id, aggregate_id, aggregate_type, creation_ts, state, version, prev_id, content, creation_date, title, saga_state) FROM stdin;
\.


--
-- Data for Name: saga_quiz; Type: TABLE DATA; Schema: public; Owner: tomasnascimento
--

COPY public.saga_quiz (id, aggregate_id, aggregate_type, creation_ts, state, version, prev_id, available_date, conclusion_date, creation_date, quiz_type, results_date, title, saga_state) FROM stdin;
\.


--
-- Data for Name: saga_quiz_answer; Type: TABLE DATA; Schema: public; Owner: tomasnascimento
--

COPY public.saga_quiz_answer (id, aggregate_id, aggregate_type, creation_ts, state, version, prev_id, answer_date, completed, creation_date, saga_state) FROM stdin;
\.


--
-- Data for Name: saga_topic; Type: TABLE DATA; Schema: public; Owner: tomasnascimento
--

COPY public.saga_topic (id, aggregate_id, aggregate_type, creation_ts, state, version, prev_id, name, saga_state) FROM stdin;
\.


--
-- Data for Name: saga_tournament; Type: TABLE DATA; Schema: public; Owner: tomasnascimento
--

COPY public.saga_tournament (id, aggregate_id, aggregate_type, creation_ts, state, version, prev_id, cancelled, end_time, number_of_questions, start_time, saga_state) FROM stdin;
\.


--
-- Data for Name: saga_user; Type: TABLE DATA; Schema: public; Owner: tomasnascimento
--

COPY public.saga_user (id, aggregate_id, aggregate_type, creation_ts, state, version, prev_id, active, name, role, username, saga_state) FROM stdin;
\.


--
-- Data for Name: topic_course; Type: TABLE DATA; Schema: public; Owner: tomasnascimento
--

COPY public.topic_course (id, course_aggregate_id, course_version, topic_id) FROM stdin;
\.


--
-- Data for Name: tournament_course_execution; Type: TABLE DATA; Schema: public; Owner: tomasnascimento
--

COPY public.tournament_course_execution (id, course_execution_acronym, course_execution_aggregate_id, course_execution_course_id, course_execution_status, course_execution_version, tournament_id) FROM stdin;
\.


--
-- Data for Name: tournament_creator; Type: TABLE DATA; Schema: public; Owner: tomasnascimento
--

COPY public.tournament_creator (id, creator_aggregate_id, creator_name, creator_state, creator_username, creator_version, tournament_id) FROM stdin;
\.


--
-- Data for Name: tournament_participant; Type: TABLE DATA; Schema: public; Owner: tomasnascimento
--

COPY public.tournament_participant (id, enroll_time, participant_aggregate_id, participant_name, participant_username, participant_version, state, tournament_id) FROM stdin;
\.


--
-- Data for Name: tournament_participant_quiz_answer; Type: TABLE DATA; Schema: public; Owner: tomasnascimento
--

COPY public.tournament_participant_quiz_answer (id, answered, number_of_answered, number_of_correct, quiz_answer_aggregate_id, quiz_answer_version, tournament_participant_id) FROM stdin;
\.


--
-- Data for Name: tournament_quiz; Type: TABLE DATA; Schema: public; Owner: tomasnascimento
--

COPY public.tournament_quiz (id, quiz_aggregate_id, quiz_version, tournament_id) FROM stdin;
\.


--
-- Data for Name: tournament_topic; Type: TABLE DATA; Schema: public; Owner: tomasnascimento
--

COPY public.tournament_topic (id, state, topic_aggregate_id, topic_course_aggregate_id, topic_name, topic_version, tournament_id) FROM stdin;
\.


--
-- Data for Name: version; Type: TABLE DATA; Schema: public; Owner: tomasnascimento
--

COPY public.version (id, number_of_decrements, version_number) FROM stdin;
-47	0	0
\.


--
-- Name: aggregate_id_generator_seq; Type: SEQUENCE SET; Schema: public; Owner: tomasnascimento
--

SELECT pg_catalog.setval('public.aggregate_id_generator_seq', 1, false);


--
-- Name: aggregate_seq; Type: SEQUENCE SET; Schema: public; Owner: tomasnascimento
--

SELECT pg_catalog.setval('public.aggregate_seq', 1, false);


--
-- Name: answer_course_execution_seq; Type: SEQUENCE SET; Schema: public; Owner: tomasnascimento
--

SELECT pg_catalog.setval('public.answer_course_execution_seq', 1, false);


--
-- Name: answer_student_seq; Type: SEQUENCE SET; Schema: public; Owner: tomasnascimento
--

SELECT pg_catalog.setval('public.answer_student_seq', 1, false);


--
-- Name: answered_quiz_seq; Type: SEQUENCE SET; Schema: public; Owner: tomasnascimento
--

SELECT pg_catalog.setval('public.answered_quiz_seq', 1, false);


--
-- Name: course_execution_course_seq; Type: SEQUENCE SET; Schema: public; Owner: tomasnascimento
--

SELECT pg_catalog.setval('public.course_execution_course_seq', 1, false);


--
-- Name: course_execution_student_seq; Type: SEQUENCE SET; Schema: public; Owner: tomasnascimento
--

SELECT pg_catalog.setval('public.course_execution_student_seq', 1, false);


--
-- Name: event_seq; Type: SEQUENCE SET; Schema: public; Owner: tomasnascimento
--

SELECT pg_catalog.setval('public.event_seq', 1, false);


--
-- Name: option_seq; Type: SEQUENCE SET; Schema: public; Owner: tomasnascimento
--

SELECT pg_catalog.setval('public.option_seq', 1, false);


--
-- Name: question_answer_seq; Type: SEQUENCE SET; Schema: public; Owner: tomasnascimento
--

SELECT pg_catalog.setval('public.question_answer_seq', 1, false);


--
-- Name: question_course_seq; Type: SEQUENCE SET; Schema: public; Owner: tomasnascimento
--

SELECT pg_catalog.setval('public.question_course_seq', 1, false);


--
-- Name: question_topic_seq; Type: SEQUENCE SET; Schema: public; Owner: tomasnascimento
--

SELECT pg_catalog.setval('public.question_topic_seq', 1, false);


--
-- Name: quiz_course_execution_seq; Type: SEQUENCE SET; Schema: public; Owner: tomasnascimento
--

SELECT pg_catalog.setval('public.quiz_course_execution_seq', 1, false);


--
-- Name: quiz_question_seq; Type: SEQUENCE SET; Schema: public; Owner: tomasnascimento
--

SELECT pg_catalog.setval('public.quiz_question_seq', 1, false);


--
-- Name: topic_course_seq; Type: SEQUENCE SET; Schema: public; Owner: tomasnascimento
--

SELECT pg_catalog.setval('public.topic_course_seq', 1, false);


--
-- Name: tournament_course_execution_seq; Type: SEQUENCE SET; Schema: public; Owner: tomasnascimento
--

SELECT pg_catalog.setval('public.tournament_course_execution_seq', 1, false);


--
-- Name: tournament_creator_seq; Type: SEQUENCE SET; Schema: public; Owner: tomasnascimento
--

SELECT pg_catalog.setval('public.tournament_creator_seq', 1, false);


--
-- Name: tournament_participant_quiz_answer_seq; Type: SEQUENCE SET; Schema: public; Owner: tomasnascimento
--

SELECT pg_catalog.setval('public.tournament_participant_quiz_answer_seq', 1, false);


--
-- Name: tournament_participant_seq; Type: SEQUENCE SET; Schema: public; Owner: tomasnascimento
--

SELECT pg_catalog.setval('public.tournament_participant_seq', 1, false);


--
-- Name: tournament_quiz_seq; Type: SEQUENCE SET; Schema: public; Owner: tomasnascimento
--

SELECT pg_catalog.setval('public.tournament_quiz_seq', 1, false);


--
-- Name: tournament_topic_seq; Type: SEQUENCE SET; Schema: public; Owner: tomasnascimento
--

SELECT pg_catalog.setval('public.tournament_topic_seq', 1, false);


--
-- Name: version_seq; Type: SEQUENCE SET; Schema: public; Owner: tomasnascimento
--

SELECT pg_catalog.setval('public.version_seq', 1, false);


--
-- Name: aggregate_id_generator aggregate_id_generator_pkey; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.aggregate_id_generator
    ADD CONSTRAINT aggregate_id_generator_pkey PRIMARY KEY (id);


--
-- Name: answer_course_execution answer_course_execution_pkey; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.answer_course_execution
    ADD CONSTRAINT answer_course_execution_pkey PRIMARY KEY (id);


--
-- Name: answer_student answer_student_pkey; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.answer_student
    ADD CONSTRAINT answer_student_pkey PRIMARY KEY (id);


--
-- Name: answered_quiz answered_quiz_pkey; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.answered_quiz
    ADD CONSTRAINT answered_quiz_pkey PRIMARY KEY (id);


--
-- Name: causal_course_execution causal_course_execution_pkey; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.causal_course_execution
    ADD CONSTRAINT causal_course_execution_pkey PRIMARY KEY (id);


--
-- Name: causal_course causal_course_pkey; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.causal_course
    ADD CONSTRAINT causal_course_pkey PRIMARY KEY (id);


--
-- Name: causal_question causal_question_pkey; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.causal_question
    ADD CONSTRAINT causal_question_pkey PRIMARY KEY (id);


--
-- Name: causal_quiz_answer causal_quiz_answer_pkey; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.causal_quiz_answer
    ADD CONSTRAINT causal_quiz_answer_pkey PRIMARY KEY (id);


--
-- Name: causal_quiz causal_quiz_pkey; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.causal_quiz
    ADD CONSTRAINT causal_quiz_pkey PRIMARY KEY (id);


--
-- Name: causal_topic causal_topic_pkey; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.causal_topic
    ADD CONSTRAINT causal_topic_pkey PRIMARY KEY (id);


--
-- Name: causal_tournament causal_tournament_pkey; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.causal_tournament
    ADD CONSTRAINT causal_tournament_pkey PRIMARY KEY (id);


--
-- Name: causal_user causal_user_pkey; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.causal_user
    ADD CONSTRAINT causal_user_pkey PRIMARY KEY (id);


--
-- Name: course_execution_course course_execution_course_pkey; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.course_execution_course
    ADD CONSTRAINT course_execution_course_pkey PRIMARY KEY (id);


--
-- Name: course_execution_student course_execution_student_pkey; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.course_execution_student
    ADD CONSTRAINT course_execution_student_pkey PRIMARY KEY (id);


--
-- Name: event event_pkey; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.event
    ADD CONSTRAINT event_pkey PRIMARY KEY (id);


--
-- Name: option option_pkey; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.option
    ADD CONSTRAINT option_pkey PRIMARY KEY (id);


--
-- Name: question_answer question_answer_pkey; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.question_answer
    ADD CONSTRAINT question_answer_pkey PRIMARY KEY (id);


--
-- Name: question_course question_course_pkey; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.question_course
    ADD CONSTRAINT question_course_pkey PRIMARY KEY (id);


--
-- Name: question_topic question_topic_pkey; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.question_topic
    ADD CONSTRAINT question_topic_pkey PRIMARY KEY (id);


--
-- Name: quiz_course_execution quiz_course_execution_pkey; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.quiz_course_execution
    ADD CONSTRAINT quiz_course_execution_pkey PRIMARY KEY (id);


--
-- Name: quiz_question quiz_question_pkey; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.quiz_question
    ADD CONSTRAINT quiz_question_pkey PRIMARY KEY (id);


--
-- Name: saga_course_execution saga_course_execution_pkey; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.saga_course_execution
    ADD CONSTRAINT saga_course_execution_pkey PRIMARY KEY (id);


--
-- Name: saga_course saga_course_pkey; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.saga_course
    ADD CONSTRAINT saga_course_pkey PRIMARY KEY (id);


--
-- Name: saga_question saga_question_pkey; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.saga_question
    ADD CONSTRAINT saga_question_pkey PRIMARY KEY (id);


--
-- Name: saga_quiz_answer saga_quiz_answer_pkey; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.saga_quiz_answer
    ADD CONSTRAINT saga_quiz_answer_pkey PRIMARY KEY (id);


--
-- Name: saga_quiz saga_quiz_pkey; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.saga_quiz
    ADD CONSTRAINT saga_quiz_pkey PRIMARY KEY (id);


--
-- Name: saga_topic saga_topic_pkey; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.saga_topic
    ADD CONSTRAINT saga_topic_pkey PRIMARY KEY (id);


--
-- Name: saga_tournament saga_tournament_pkey; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.saga_tournament
    ADD CONSTRAINT saga_tournament_pkey PRIMARY KEY (id);


--
-- Name: saga_user saga_user_pkey; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.saga_user
    ADD CONSTRAINT saga_user_pkey PRIMARY KEY (id);


--
-- Name: topic_course topic_course_pkey; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.topic_course
    ADD CONSTRAINT topic_course_pkey PRIMARY KEY (id);


--
-- Name: tournament_course_execution tournament_course_execution_pkey; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.tournament_course_execution
    ADD CONSTRAINT tournament_course_execution_pkey PRIMARY KEY (id);


--
-- Name: tournament_creator tournament_creator_pkey; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.tournament_creator
    ADD CONSTRAINT tournament_creator_pkey PRIMARY KEY (id);


--
-- Name: tournament_participant tournament_participant_pkey; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.tournament_participant
    ADD CONSTRAINT tournament_participant_pkey PRIMARY KEY (id);


--
-- Name: tournament_participant_quiz_answer tournament_participant_quiz_answer_pkey; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.tournament_participant_quiz_answer
    ADD CONSTRAINT tournament_participant_quiz_answer_pkey PRIMARY KEY (id);


--
-- Name: tournament_quiz tournament_quiz_pkey; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.tournament_quiz
    ADD CONSTRAINT tournament_quiz_pkey PRIMARY KEY (id);


--
-- Name: tournament_topic tournament_topic_pkey; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.tournament_topic
    ADD CONSTRAINT tournament_topic_pkey PRIMARY KEY (id);


--
-- Name: tournament_course_execution uk3n8c8qmkt7whibmp56i378uli; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.tournament_course_execution
    ADD CONSTRAINT uk3n8c8qmkt7whibmp56i378uli UNIQUE (tournament_id);


--
-- Name: tournament_creator uk6bgy7kl5cs69sg9wofe71o3j9; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.tournament_creator
    ADD CONSTRAINT uk6bgy7kl5cs69sg9wofe71o3j9 UNIQUE (tournament_id);


--
-- Name: tournament_participant_quiz_answer uk758un9jyqj5ao7hp93yakqsla; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.tournament_participant_quiz_answer
    ADD CONSTRAINT uk758un9jyqj5ao7hp93yakqsla UNIQUE (tournament_participant_id);


--
-- Name: course_execution_course uk7msuftj3l1jn3pwun98r89ljd; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.course_execution_course
    ADD CONSTRAINT uk7msuftj3l1jn3pwun98r89ljd UNIQUE (course_execution_id);


--
-- Name: quiz_course_execution uk99296y80po1gge71cfhj4kwwe; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.quiz_course_execution
    ADD CONSTRAINT uk99296y80po1gge71cfhj4kwwe UNIQUE (quiz_id);


--
-- Name: topic_course ukc0avk8huyg6jb13jo5bsyhvsy; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.topic_course
    ADD CONSTRAINT ukc0avk8huyg6jb13jo5bsyhvsy UNIQUE (topic_id);


--
-- Name: answer_student ukd0gq5b068d4sn41l307b6ik40; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.answer_student
    ADD CONSTRAINT ukd0gq5b068d4sn41l307b6ik40 UNIQUE (quiz_answer_id);


--
-- Name: answer_course_execution ukeod4t14u9r2t0qpwyexo6onob; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.answer_course_execution
    ADD CONSTRAINT ukeod4t14u9r2t0qpwyexo6onob UNIQUE (quiz_answer_id);


--
-- Name: answered_quiz ukl9wefngpm9eetq410vw82nmnu; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.answered_quiz
    ADD CONSTRAINT ukl9wefngpm9eetq410vw82nmnu UNIQUE (quiz_answer_id);


--
-- Name: question_course ukp7y50pyn8nyuqdr8tj18eqbxd; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.question_course
    ADD CONSTRAINT ukp7y50pyn8nyuqdr8tj18eqbxd UNIQUE (question_id);


--
-- Name: tournament_quiz uktldfqq6ngp4cp7bvwd6gxrsag; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.tournament_quiz
    ADD CONSTRAINT uktldfqq6ngp4cp7bvwd6gxrsag UNIQUE (tournament_id);


--
-- Name: version version_pkey; Type: CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.version
    ADD CONSTRAINT version_pkey PRIMARY KEY (id);


--
-- Name: answered_quiz_quiz_questions_aggregate_ids fk7hvtlw9p2oy9ius120vt8fry8; Type: FK CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.answered_quiz_quiz_questions_aggregate_ids
    ADD CONSTRAINT fk7hvtlw9p2oy9ius120vt8fry8 FOREIGN KEY (answered_quiz_id) REFERENCES public.answered_quiz(id);


--
-- Name: tournament_participant_quiz_answer fkdk8xeehqggianmhfbra77588b; Type: FK CONSTRAINT; Schema: public; Owner: tomasnascimento
--

ALTER TABLE ONLY public.tournament_participant_quiz_answer
    ADD CONSTRAINT fkdk8xeehqggianmhfbra77588b FOREIGN KEY (tournament_participant_id) REFERENCES public.tournament_participant(id);


--
-- PostgreSQL database dump complete
--

