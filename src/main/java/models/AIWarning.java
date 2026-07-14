package models;

import java.time.LocalDateTime;

public class AIWarning {
    private int warningId;
    private int recordId;
    private String riskLevel;      // LOW / MEDIUM / HIGH
    private String warningMessage;
    private String suggestedAction;
    private double aiScore;
    private LocalDateTime generatedAt;
    private boolean reviewedByDoctor;

    public AIWarning() {}

    public int getWarningId()                    { return warningId; }
    public void setWarningId(int v)              { this.warningId = v; }

    public int getRecordId()                     { return recordId; }
    public void setRecordId(int v)               { this.recordId = v; }

    public String getRiskLevel()                 { return riskLevel; }
    public void setRiskLevel(String v)           { this.riskLevel = v; }

    public String getWarningMessage()            { return warningMessage; }
    public void setWarningMessage(String v)      { this.warningMessage = v; }

    public String getSuggestedAction()           { return suggestedAction; }
    public void setSuggestedAction(String v)     { this.suggestedAction = v; }

    public double getAiScore()                   { return aiScore; }
    public void setAiScore(double v)             { this.aiScore = v; }

    public LocalDateTime getGeneratedAt()             { return generatedAt; }
    public void setGeneratedAt(LocalDateTime v)       { this.generatedAt = v; }

    public boolean isReviewedByDoctor()          { return reviewedByDoctor; }
    public void setReviewedByDoctor(boolean v)   { this.reviewedByDoctor = v; }
}
