package com.ballpark.ticketing.game;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(uniqueConstraints = @UniqueConstraint(
		name = "uk_seat_section_row_seat",
		columnNames = {"section_id", "row_no", "seat_no"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seat {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "section_id", nullable = false)
	private Section section;

	@Column(name = "row_no", nullable = false)
	private int rowNo;

	@Column(name = "seat_no", nullable = false)
	private int seatNo;

	public Seat(Section section, int rowNo, int seatNo) {
		this.section = section;
		this.rowNo = rowNo;
		this.seatNo = seatNo;
	}
}
