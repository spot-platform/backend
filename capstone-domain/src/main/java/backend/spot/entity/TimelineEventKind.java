package backend.spot.entity;

public enum TimelineEventKind {
	CREATED,
	MATCHED,
	COMPLETED,
	CANCELLED,
	COMMENT,
	SETTLEMENT_REQUESTED,
	SETTLEMENT_APPROVED
}
