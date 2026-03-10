resource "tls_private_key" "this" {
  algorithm = "RSA"
  rsa_bits  = 4096
}

resource "aws_key_pair" "this" {
  key_name   = var.key_pair_name
  public_key = tls_private_key.this.public_key_openssh

  tags = merge(var.tags, {
    Name = var.key_pair_name
  })
}

resource "local_sensitive_file" "private_key" {
  filename        = "${var.keys_output_dir}/${var.key_pair_name}.pem"
  content         = tls_private_key.this.private_key_pem
  file_permission = "0400"
}

resource "local_file" "public_key" {
  filename        = "${var.keys_output_dir}/${var.key_pair_name}.pub"
  content         = tls_private_key.this.public_key_openssh
  file_permission = "0644"
}
