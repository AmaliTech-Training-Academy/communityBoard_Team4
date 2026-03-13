output "cloudtrail_bucket_name" {
  value = aws_s3_bucket.cloudtrail.bucket
}

output "cloudtrail_trail_arn" {
  value = aws_cloudtrail.this.arn
}

output "guardduty_detector_id" {
  value = aws_guardduty_detector.this.id
}

output "ses_sender_identity_arn" {
  value = aws_sesv2_email_identity.app_sender.arn
}
